package com.benito.sales.service;

import com.benito.sales.dto.OrderMessageDTO;
import com.benito.sales.dto.restClientDTO.BalanceRequestDTO;
import com.benito.sales.entity.OrderStatus;
import com.benito.sales.entity.OrdersPayments;
import com.benito.sales.repository.OrdersPaymentsRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderProcessorServiceTest {

    @Test
    void processPayment_chargesTheClient_newOrder()
    {
        //ARRANGE
        OrdersPaymentsRepository fakeRepository = mock(OrdersPaymentsRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        Emitter<String> fakeEmitter = mock(Emitter.class);
        ServiceTokenProvider fakeTokenProvider = mock(ServiceTokenProvider.class);


        OrderProcessorService service = new OrderProcessorService(fakeRepository, fakeClientsApi, fakeEmitter,
                                                                    fakeTokenProvider, new ObjectMapper());

        OrderMessageDTO order = new OrderMessageDTO("client-1", "order-1", new BigDecimal("100.00"));

        //THE ORDER WAS NEVER PROCESSED BEFORE
        when(fakeRepository.findByOrderId("order-1")).thenReturn(null);
        when(fakeTokenProvider.getAuthorizationToken()).thenReturn("Bearer token");

        //ACT
        service.processPayment(order);

        //ASSERT: THE CLIENT WAS CHARGED ONCE AND THE RESULT WAS PUBLISHED
        verify(fakeClientsApi, times(1)).debitBalance(eq("Bearer token"),
                                                                            eq("client-1"),
                                                                            any(BalanceRequestDTO.class));
    }

    @Test
    void processPayment_doesNotChargeAgain_orderAlreadyProcessed()
    {
        //ARRANGE
        OrdersPaymentsRepository fakeRepository = mock(OrdersPaymentsRepository.class);
        ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
        Emitter<String> fakeEmitter = mock(Emitter.class);
        ServiceTokenProvider fakeTokenProvider = mock(ServiceTokenProvider.class);

        OrderProcessorService service = new OrderProcessorService(fakeRepository, fakeClientsApi, fakeEmitter,
                                                                  fakeTokenProvider, new ObjectMapper());

        OrderMessageDTO order = new OrderMessageDTO("client-1", "order-1", new BigDecimal("100.00"));

        //THE SAME MESSAGE ARRIVES AGAIN, BUT THIS ORDER WAS ALREADY COMPLETED
        OrdersPayments payment = new OrdersPayments();
        payment.orderId = "order-1";
        payment.status = OrderStatus.COMPLETED;

        when(fakeRepository.findByOrderId("order-1")).thenReturn(payment);

        //ACT
        service.processPayment(order);

        //ASSERT: THE CLIENT IS NOT CHARGED TWICE
        verify(fakeClientsApi, never()).debitBalance(anyString(), anyString(), any(BalanceRequestDTO.class));
    }
}