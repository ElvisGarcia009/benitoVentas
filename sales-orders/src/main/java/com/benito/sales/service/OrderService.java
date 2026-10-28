package com.benito.sales.service;

import com.benito.sales.dto.*;
import com.benito.sales.dto.restClientDTO.BalanceRequestDTO;
import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderProductRecord;
import com.benito.sales.exception.*;
import com.benito.sales.repository.OrderRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.benito.sales.restClient.ProductsApiClient;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OrderService {

    private final OrderRepository _orderRepository;
    private final ClientsApiClient _clientsApi;
    private final ProductsApiClient _productsApi;

    public OrderService(OrderRepository orderRepository,
                        @RestClient ClientsApiClient clientsApi,
                        @RestClient ProductsApiClient productsApi)
    {
        this._orderRepository = orderRepository;
        this._clientsApi = clientsApi;
        this._productsApi = productsApi;
    }


    //GET ALL ORDERS, PAGINATED
    public OrdersPaginatedDTO getAllOrders(int page, int pageSize)
    {
        PanacheQuery<Order> query = _orderRepository.find("status", Sort.by("createdAt").descending(), true)
                .page(page, pageSize);

        List<OrderResponseDTO> orders = query.stream().map(OrderResponseDTO::TurnIntoDTO).toList();
        int totalPages = query.pageCount();
        long totalOrders = query.count();

        return new OrdersPaginatedDTO(orders, page, pageSize, totalPages, totalOrders);
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
        int totalPages = query.pageCount();
        long totalOrders = query.count();

        return new OrdersPaginatedDTO(orders, page, pageSize, totalPages, totalOrders);
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


    //CREATE A PURCHASE ORDER
    public OrderResponseDTO createPurchaseOrder(OrderRequestDTO requestOrder, String rawToken, String username)
    {
        String authorization = "Bearer " + rawToken;

        //GET THE CLIENT INFORMATION
        ClientResponseDTO clientFound = getClient(authorization, requestOrder.clientId);

        //GET THE PRODUCTS INFORMATION AND CALCULATE THE TOTALS
        List<OrderProductRecord> productsWithInfo = new ArrayList<>();
        BigDecimal orderTotal = BigDecimal.ZERO;

        for (OrderProductRecordRequestDTO requestProduct : requestOrder.products)
        {
            ProductResponseDTO productFound = getProduct(requestProduct.productId());

            BigDecimal totalPerProduct = productFound.price.multiply(BigDecimal.valueOf(requestProduct.quantity()));
            orderTotal = orderTotal.add(totalPerProduct);

            OrderProductRecord fillProductInformation = new OrderProductRecord(productFound.id,
                    productFound.name,
                    productFound.price,
                    requestProduct.quantity(),
                    totalPerProduct);

            productsWithInfo.add(fillProductInformation);
        }

        //MAKE A NEW ORDER
        Order newOrder = new Order();
        newOrder.clientId = clientFound.id;
        newOrder.userId = clientFound.userId;
        newOrder.products = productsWithInfo;
        newOrder.orderTotal = orderTotal;

        //AUDIT FIELDS
        newOrder.status = true;
        newOrder.createdBy = username;
        newOrder.createdAt = Instant.now();

        _orderRepository.persist(newOrder);

        //CHARGE THE CLIENT
        try
        {
            chargeClient(authorization, clientFound.id, orderTotal);
        }
        catch (RuntimeException exception)
        {
            //COMPENSATION: THE CLIENT COULD NOT BE CHARGED, SO THE ORDER IS DELETED
            _orderRepository.delete(newOrder);
            throw exception;
        }

        return OrderResponseDTO.TurnIntoDTO(newOrder);
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

    //CALLS THE CLIENTS SERVICE TO GET A CLIENT
    private ClientResponseDTO getClient(String authorization, String clientId)
    {
        try
        {
            return _clientsApi.getClientById(authorization, clientId);
        }
        catch (WebApplicationException exception)   //THE CLIENT SERVICE ANSWERED WITH AN ERROR
        {
            int status = exception.getResponse().getStatus();

           switch(status)
           {
               case (404): throw new ClientNotFoundException(clientId);
               case (403): throw new ClientAccessDeniedException();
               default: throw new ExternalServiceException("clients");
           }
        }
        catch (ProcessingException exception) //THE CLIENT SERVICE DID NOT ANSWER
        {
            throw new ServiceNotAvailableException("clients");
        }
    }

    //CALLS THE PRODUCT SERVICE TO GET A PRODUCT
    private ProductResponseDTO getProduct(String productId)
    {
        try
        {
            return _productsApi.getProductById(productId);
        }
        catch (WebApplicationException exception)
        {
            int status = exception.getResponse().getStatus();

            switch (status)
            {
                case (404): throw new ProductNotFoundException(productId);
                case (400): throw new InvalidIdException(productId);
                default: throw new ExternalServiceException("products");
            }
        }
        catch (ProcessingException exception)
        {
            throw new ServiceNotAvailableException("products");
        }
    }

    //CALLS THE CLIENT SERVICE TO SUBTRACT THE ORDER TOTAL FROM THE CLIENT'S BALANCE
    private void chargeClient(String authorization, String clientId, BigDecimal amount)
    {
        try
        {
            _clientsApi.debitBalance(authorization, clientId, new BalanceRequestDTO(amount));
        }
        catch (WebApplicationException exception)
        {
            int status = exception.getResponse().getStatus();

            if (status == 422)
                throw new InsufficientBalanceException();

            if (status == 404)
                throw new ClientNotFoundException(clientId);

            if (status == 403)
                throw new ClientAccessDeniedException();

            throw new ExternalServiceException("clients");
        }
        catch (ProcessingException exception)
        {
            throw new ServiceNotAvailableException("clients");
        }
    }
}
