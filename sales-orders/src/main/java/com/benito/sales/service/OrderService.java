package com.benito.sales.service;

import com.benito.sales.dto.*;
import com.benito.sales.dto.restClientDTO.BalanceRequestDTO;
import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderProductRecord;
import com.benito.sales.entity.OrderState;
import com.benito.sales.exception.*;
import com.benito.sales.repository.OrderRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.benito.sales.restClient.ProductsApiClient;
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
import java.util.List;

@ApplicationScoped
public class OrderService {

    private final static Logger LOG = Logger.getLogger(OrderService.class);

    //DEPENDENCY INJECTION -> Repository, Rest Clients, Kafka emitter & service token
    private final OrderRepository _orderRepository;
    private final ClientsApiClient _clientsApi;
    private final ProductsApiClient _productsApi;
    private final Emitter<String> _emitter;
    private final ServiceTokenProvider _serviceTokenProvider;

    public OrderService(OrderRepository orderRepository,
                        @RestClient ClientsApiClient clientsApi,
                        @RestClient ProductsApiClient productsApi,
                        @Channel("orders-out") Emitter<String> emitter,
                        ServiceTokenProvider serviceTokenProvider)
    {
        this._orderRepository = orderRepository;
        this._clientsApi = clientsApi;
        this._productsApi = productsApi;
        this._emitter = emitter;
        this._serviceTokenProvider = serviceTokenProvider;
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


    //CREATE A PURCHASE ORDER (SAVE IT AS PENDING AND QUEUE IT IN KAFKA)
    public TemporalOrderResponseDTO createPurchaseOrder(OrderRequestDTO requestOrder,
                                                        String rawToken, String username)
    {
        ClientResponseDTO clientFound = getClient("Bearer " + rawToken, requestOrder.clientId);

        List<OrderProductRecord> requestedProducts = new ArrayList<>();
        for (OrderProductRecordRequestDTO requestProduct : requestOrder.products)
        {
            requestedProducts.add(new OrderProductRecord(requestProduct.productId(),
                                                         null,
                                                         null,
                                                         requestProduct.quantity(),
                                                         null));
        }

        Order newOrder = new Order();
        newOrder.clientId = clientFound.id;
        newOrder.userId = clientFound.userId;
        newOrder.products = requestedProducts;
        newOrder.orderStatus = OrderState.PENDING;

        //AUDIT FIELDS
        newOrder.status = true;
        newOrder.createdBy = username;
        newOrder.createdAt = Instant.now();

        _orderRepository.persist(newOrder);

        //NOTE: MESSAGE WITH KEY AND VALUE SO ALL THE ORDERS OF A CLIENT GO TO THE SAME PARTITION AND ARE PROCESSED IN ORDER
        _emitter.send(KafkaRecord.of(newOrder.clientId, newOrder.id.toString()));

        return TemporalOrderResponseDTO.TurnIntoDTO(newOrder);
    }


    //PROCESS A QUEUED ORDER (CALLED BY THE KAFKA CONSUMER)
    public void processOrder(ObjectId orderId)
    {
        Order order = _orderRepository.findById(orderId);

        //IF THE ORDER WAS DEACTIVATED OR ALREADY PROCESSED, THERE IS NOTHING TO DO
        if (order == null || !order.status || order.orderStatus != OrderState.PENDING)
            return;

        try
        {
            //GET THE PRODUCTS INFORMATION AND CALCULATE THE TOTALS
            List<OrderProductRecord> productsWithInfo = new ArrayList<>();
            BigDecimal orderTotal = BigDecimal.ZERO;

            for (OrderProductRecord requestedProduct : order.products)
            {
                ProductResponseDTO productFound = getProduct(requestedProduct.productId());

                BigDecimal totalPerProduct = productFound.price.multiply(BigDecimal.valueOf(requestedProduct.quantity()));
                orderTotal = orderTotal.add(totalPerProduct);

                productsWithInfo.add(new OrderProductRecord(productFound.id,
                        productFound.name,
                        productFound.price,
                        requestedProduct.quantity(),
                        totalPerProduct));
            }

            order.products = productsWithInfo;
            order.orderTotal = orderTotal;

            //CHARGE THE CLIENT WITH THE SERVICE OWN TOKEN
            chargeClient(_serviceTokenProvider.getAuthorizationToken(), order.clientId, orderTotal);

            order.orderStatus = OrderState.COMPLETED;
        }
        catch (InsufficientBalanceException | ProductNotFoundException | InvalidIdException
               | ClientNotFoundException | ClientAccessDeniedException exception)
        {
            //BUSINESS ERRORS: THE ORDER IS REJECTED AND THE CONSUMER KEEPS GOING
            order.orderStatus = OrderState.REJECTED;
            order.rejectionReason = exception.getMessage();
        }
        catch (RuntimeException exception)
        {
            //UNEXPECTED ERRORS: THE ORDER IS REJECTED AND THE MESSAGE GOES TO THE DEAD LETTER QUEUE
            LOG.error("Unexpected error processing order " + orderId, exception);

            order.orderStatus = OrderState.REJECTED;
            order.rejectionReason = "The order could not be processed, please try again later";
            saveProcessedOrder(order);

            throw exception;
        }

        saveProcessedOrder(order);
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




    //HELPERS
    private Order findValidOrder(ObjectId id)
    {
        Order orderContainer = _orderRepository.findById(id);

        if (orderContainer == null || !orderContainer.status)
            throw new OrderNotFoundException(id);

        return orderContainer;
    }

    private void saveProcessedOrder(Order order)
    {
        order.updatedBy = "orders-service";
        order.updatedAt = Instant.now();

        _orderRepository.update(order);
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

    //CALLS THE PRODUCTS SERVICE TO GET A PRODUCT
    private ProductResponseDTO getProduct(String productId)
    {
        try
        {
            return _productsApi.getProductById(productId);
        }
        catch (WebApplicationException exception)
        {
            switch (exception.getResponse().getStatus())
            {
                case 404: throw new ProductNotFoundException(productId);
                case 400: throw new InvalidIdException(productId);
                default: throw new ExternalServiceException("products");
            }
        }
        catch (ProcessingException exception)
        {
            throw new ServiceNotAvailableException("products");
        }
    }

    //CALLS THE CLIENTS SERVICE TO SUBTRACT THE ORDER TOTAL FROM THE CLIENT'S BALANCE
    private void chargeClient(String authorization, String clientId, BigDecimal amount)
    {
        try
        {
            _clientsApi.debitBalance(authorization, clientId, new BalanceRequestDTO(amount));
        }
        catch (WebApplicationException exception)
        {
            switch (exception.getResponse().getStatus())
            {
                case 422: throw new InsufficientBalanceException();
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
}