package com.benito.sales.service;

import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderProductRecord;
import com.benito.sales.entity.OrderState;
import com.benito.sales.repository.OrderRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.benito.sales.restClient.ProductsApiClient;
import jakarta.ws.rs.ProcessingException;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    //TESTING VITAL FUNCTIONS IN THE SERVICE

    @Test
    void processOrder_completesTheOrder_everythingIsOk()
    {
        //ARRANGE
        OrderRepository fakeRepository = mock(OrderRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        ProductsApiClient fakeProductsApi = mock(ProductsApiClient.class);
        Emitter<String> fakeEmitter = mock(Emitter.class);
        ServiceTokenProvider fakeTokenProvider = mock(ServiceTokenProvider.class);

        OrderService service = new OrderService(fakeRepository, fakeClientsApi, fakeProductsApi,
                                                fakeEmitter, fakeTokenProvider);

        ObjectId id = new ObjectId();

        Order order = new Order();
        order.id = id;
        order.clientId = "client-1";
        order.status = true;
        order.orderStatus = OrderState.PENDING;
        order.products = List.of(new OrderProductRecord("p1", null, null, 2, null));

        ProductResponseDTO product = new ProductResponseDTO();
        product.id = "p1";
        product.name = "Mouse";
        product.price = new BigDecimal("25.00");

        when(fakeRepository.findById(id)).thenReturn(order);
        when(fakeTokenProvider.getAuthorizationToken()).thenReturn("Bearer token");
        when(fakeProductsApi.getProductById("Bearer token", "p1")).thenReturn(product);

        //ACT
        service.processOrder(id);

        //ASSERT
        assertAll(() -> assertEquals(OrderState.COMPLETED, order.orderStatus),
                () -> assertEquals(new BigDecimal("50.00"), order.orderTotal),
                () -> assertEquals("Mouse", order.products.getFirst().productName()));
    }

    @Test
    void processOrder_rejectsTheOrder_productsServiceIsDown()
    {
        //ARRANGE
        OrderRepository fakeRepository = mock(OrderRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        ProductsApiClient fakeProductsApi = mock(ProductsApiClient.class);
        Emitter<String> fakeEmitter = mock(Emitter.class);
        ServiceTokenProvider fakeTokenProvider = mock(ServiceTokenProvider.class);

        OrderService service = new OrderService(fakeRepository, fakeClientsApi, fakeProductsApi,
                                                fakeEmitter, fakeTokenProvider);

        ObjectId id = new ObjectId();

        Order order = new Order();
        order.id = id;
        order.clientId = "client-1";
        order.status = true;
        order.orderStatus = OrderState.PENDING;
        order.products = List.of(new OrderProductRecord("p1", null, null, 1, null));

        when(fakeRepository.findById(id)).thenReturn(order);
        when(fakeTokenProvider.getAuthorizationToken()).thenReturn("Bearer token");
        when(fakeProductsApi.getProductById("Bearer token", "p1")).thenThrow(ProcessingException.class);

        //ACT
        service.processOrder(id);

        //ASSERT
        assertAll(() -> assertEquals(OrderState.REJECTED, order.orderStatus),
                  () -> assertEquals("The order could not be processed, please try again later",
                                     order.rejectionReason));

        //THE CLIENT WAS NEVER CHARGED
        verify(fakeClientsApi, never()).debitBalance(any(), any(), any());
    }

    @Test
    void processOrder_doesNothing_orderAlreadyProcessed()
    {
        //ARRANGE
        OrderRepository fakeRepository = mock(OrderRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        ProductsApiClient fakeProductsApi = mock(ProductsApiClient.class);
        Emitter<String> fakeEmitter = mock(Emitter.class);
        ServiceTokenProvider fakeTokenProvider = mock(ServiceTokenProvider.class);

        OrderService service = new OrderService(fakeRepository, fakeClientsApi, fakeProductsApi,
                                                fakeEmitter, fakeTokenProvider);

        ObjectId id = new ObjectId();

        Order order = new Order();
        order.id = id;
        order.clientId = "client-1";
        order.status = true;
        order.orderStatus = OrderState.COMPLETED;
        order.products = List.of(new OrderProductRecord("p1", null, null,
                                                        1, null));

        when(fakeRepository.findById(id)).thenReturn(order);

        //ACT
        service.processOrder(id);

        //ASSERT
        assertEquals(OrderState.COMPLETED, order.orderStatus);

        //NOTHING WAS DONE: NO PRODUCTS LOOKUP AND NO CHARGE
        verify(fakeProductsApi, never()).getProductById(any(), any());
        verify(fakeClientsApi, never()).debitBalance(any(), any(), any());
    }
}