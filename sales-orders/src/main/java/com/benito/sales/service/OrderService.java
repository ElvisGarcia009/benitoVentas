package com.benito.sales.service;

import com.benito.sales.dto.*;
import com.benito.sales.dto.KafkaDTO.OrderMessageDTO;
import com.benito.sales.dto.KafkaDTO.OrderResultMessageDTO;
import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderProductRecord;
import com.benito.sales.entity.OrderState;
import com.benito.sales.exception.*;
import com.benito.sales.repository.OrderRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.benito.sales.restClient.ProductsApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OrderService {

    private final static Logger LOG = Logger.getLogger(OrderService.class);

    //DEPENDENCY INJECTION -> Repository, Rest Clients, Kafka emitter & service token
    private final OrderRepository _orderRepository;
    private final ClientsApiClient _clientsApi;
    private final ProductsApiClient _productsApi;
    private final Emitter<String> _emitter;
    private final ServiceTokenProvider _serviceTokenProvider;
    private final ObjectMapper _objectMapper;

    public OrderService(OrderRepository orderRepository,
                        @RestClient ClientsApiClient clientsApi,
                        @RestClient ProductsApiClient productsApi,
                        @Channel("orders-out") Emitter<String> emitter,
                        ServiceTokenProvider serviceTokenProvider,
                        ObjectMapper objectMapper)
    {
        this._orderRepository = orderRepository;
        this._clientsApi = clientsApi;
        this._productsApi = productsApi;
        this._emitter = emitter;
        this._serviceTokenProvider = serviceTokenProvider;
        this._objectMapper = objectMapper;
    }


    //GET ALL ORDERS, PAGINATED
    public OrdersPaginatedDTO getAllOrders(int page, int pageSize)
    {
        PanacheQuery<Order> query = _orderRepository.find("status",
                                                          Sort.by("createdAt").descending(),
                                                          true)
                                                    .page(page, pageSize);

        List<OrderResponseDTO> orders = query.stream().map(OrderResponseDTO::TurnIntoDTO).toList();

        return new OrdersPaginatedDTO(orders, page, pageSize, query.pageCount(), query.count());
    }


    //GET THE ORDERS OF THE AUTHENTICATED CLIENT, PAGINATED
    public OrdersPaginatedDTO getMyOrders(String userId, int page, int pageSize)
    {
        PanacheQuery<Order> query = _orderRepository.find("userId = ?1 and status = ?2",
                                                          Sort.by("createdAt").descending(),
                                                          userId,
                                                          true)
                                                    .page(page, pageSize);

        List<OrderResponseDTO> orders = query.stream().map(OrderResponseDTO::TurnIntoDTO).toList();

        return new OrdersPaginatedDTO(orders, page, pageSize, query.pageCount(), query.count());
    }


    //GET AN ORDER BY ID
    public OrderResponseDTO getOrderById(ObjectId id, String callerUserId, boolean isAdmin)
    {
        Order orderFound = findValidOrder(id);

        //NOTE: A CLIENT CAN ONLY SEE HIS OWN ORDERS
        if (!isAdmin && !orderFound.userId.equals(callerUserId))
            throw new OrderAccessDeniedException();

        return OrderResponseDTO.TurnIntoDTO(orderFound);
    }


    //CREATE A PURCHASE ORDER (CALCULATE THE TOTAL, SAVE IT AS PENDING AND QUEUE THE PAYMENT IN KAFKA)
    public TemporalOrderResponseDTO createPurchaseOrder(OrderRequestDTO requestOrder, String rawToken, String username)
    {
        //GET CLIENT'S INFORMATION
        ClientResponseDTO clientFound = getClient("Bearer " + rawToken, requestOrder.clientId);

        //GET THE PRODUCT'S INFORMATION
        String serviceToken = _serviceTokenProvider.getAuthorizationToken();
        List<String> productsIds = requestOrder.products.stream().map(OrderProductRecordRequestDTO::productId).toList();

        List<ProductResponseDTO> productsWithInfo = getProducts(serviceToken, productsIds);

        //MAKING A "LOCKER" SO THE ORDERS CAN CONSULT THE PRODUCT INFO QUICKLY
        Map<String, ProductResponseDTO> productsLocker = new HashMap<>();
        for (ProductResponseDTO productInfo : productsWithInfo)
        {
            productsLocker.put(productInfo.id, productInfo);
        }

        //CONTAINERS FOR THE TOTALS
        BigDecimal orderTotal = BigDecimal.ZERO;
        List<OrderProductRecord> productsWithCalculations = new ArrayList<>();

        //CALCULATIONS
        for (OrderProductRecordRequestDTO requestedProduct : requestOrder.products)
        {
            ProductResponseDTO productInfo = productsLocker.get(requestedProduct.productId());

            BigDecimal totalPerProduct = productInfo.price.multiply(BigDecimal.valueOf(requestedProduct.quantity()));
            orderTotal = orderTotal.add(totalPerProduct);

            productsWithCalculations.add(new OrderProductRecord(productInfo.id,
                    productInfo.name,
                    productInfo.price,
                    requestedProduct.quantity(),
                    totalPerProduct));
        }

        Order newOrder = new Order();
        newOrder.clientId = clientFound.id;
        newOrder.userId = clientFound.userId;
        newOrder.products = productsWithCalculations;
        newOrder.orderStatus = OrderState.PENDING;
        newOrder.orderTotal = orderTotal;

        //AUDIT FIELDS
        newOrder.status = true;
        newOrder.createdBy = username;
        newOrder.createdAt = Instant.now();

        _orderRepository.persist(newOrder);

        OrderMessageDTO messageToKafka = new OrderMessageDTO(newOrder.clientId, newOrder.id.toString(), newOrder.orderTotal);

        //NOTE: MESSAGE WITH KEY AND VALUE SO ALL THE ORDERS OF A CLIENT GO TO THE SAME PARTITION AND ARE PROCESSED IN ORDER
        _emitter.send(KafkaRecord.of(messageToKafka.clientId, toJson(messageToKafka)));

        return TemporalOrderResponseDTO.TurnIntoDTO(newOrder);
    }

    //DELETE AN ORDER (this method just deactivate the order)
    public void deleteOrder(ObjectId id, String username)
    {
        Order orderFound = findValidOrder(id);

        orderFound.status = false;
        orderFound.updatedBy = username;
        orderFound.updatedAt = Instant.now();

        _orderRepository.update(orderFound);
    }





    //APPLY THE RESULT THAT SALES-ORDER-PROCESSOR PUBLISHED (CALLED BY THE KAFKA CONSUMER)
    public void applyOrderResult(OrderResultMessageDTO result)
    {
        OrderState newState;

        try
        {
            newState = OrderState.valueOf(result.status);
        }
        catch (IllegalArgumentException | NullPointerException exception)
        {
            LOG.error("Unknown status '" + result.status + "' for order " + result.orderId + ", the message is ignored");
            return;
        }

        if (newState == OrderState.PENDING || !ObjectId.isValid(result.orderId))
        {
            LOG.error("Invalid result for order " + result.orderId + ", the message is ignored");
            return;
        }

        //IT ONLY UPDATES THE ORDER IF IT IS STILL PENDING, IN ONE ATOMIC OPERATION.
        long updated = _orderRepository.applyResult(new ObjectId(result.orderId), newState,
                result.rejectionReason, "orders-service", Instant.now());

        if (updated == 0)
            LOG.info("Order " + result.orderId + " was already updated or does not exist, nothing to do");
    }




    //HELPERS
    private Order findValidOrder(ObjectId id)
    {
        Order orderContainer = _orderRepository.findById(id);

        if (orderContainer == null || !orderContainer.status)
            throw new OrderNotFoundException(id);

        return orderContainer;
    }

    private String toJson(Object message)
    {
        try
        {
            return _objectMapper.writeValueAsString(message);
        }
        catch (JsonProcessingException exception)
        {
            throw new IllegalStateException("The Kafka message could not be serialized", exception);
        }
    }

    //CALLS THE CLIENTS SERVICE TO GET A CLIENT
    private ClientResponseDTO getClient(String authorization, String clientId)
    {
        try
        {
            return _clientsApi.getClientById(authorization, clientId);
        }
        catch (WebApplicationException exception)
        {
            switch (exception.getResponse().getStatus())
            {
                case 404: throw new ClientNotFoundException(clientId);
                case 403: throw new ClientAccessDeniedException();
                default: throw new ExternalServiceException("clients");
            }
        }
        catch (ProcessingException exception)
        {
            throw new ServiceNotAvailableException("clients");
        }
    }

    //CALL THE PRODUCT SERVICE WITH AN ARRAY OF PRODUCTS
    private List<ProductResponseDTO> getProducts(String authorization, List<String> productsIds)
    {
        try
        {
            return _productsApi.getProductsByIdsForService(authorization, productsIds);
        }
        catch (WebApplicationException exception)
        {

            switch (exception.getResponse().getStatus())
            {
                case 404: {
                    ErrorDTO error = exception.getResponse().readEntity(ErrorDTO.class);
                    throw new ProductNotFoundException(error.errorMessage);
                }
                case 400: {
                    ErrorDTO error = exception.getResponse().readEntity(ErrorDTO.class);
                    throw new InvalidProductIdException(error.errorMessage);
                }
                default: throw new ExternalServiceException("products");
            }
        }
        catch (ProcessingException exception)
        {
            throw new ServiceNotAvailableException("products");
        }
    }


}


