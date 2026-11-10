package com.benito.sales.service;

import com.benito.sales.dto.KafkaDTO.OrderResultMessageDTO;
import com.benito.sales.dto.OrderProductRecordRequestDTO;
import com.benito.sales.dto.OrderRequestDTO;
import com.benito.sales.dto.TemporalOrderResponseDTO;
import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderState;
import com.benito.sales.repository.OrderRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.benito.sales.restClient.ProductsApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    //TESTING VITAL FUNCTIONS IN THE SERVICE

    private final OrderRepository fakeRepository = mock(OrderRepository.class);
    private final ClientsApiClient fakeClientsApi = mock(ClientsApiClient.class);
    private final ProductsApiClient fakeProductsApi = mock(ProductsApiClient.class);
    @SuppressWarnings("unchecked")
    private final Emitter<String> fakeEmitter = mock(Emitter.class);
    private final ServiceTokenProvider fakeTokenProvider = mock(ServiceTokenProvider.class);

    private final OrderService service = new OrderService(fakeRepository, fakeClientsApi, fakeProductsApi,
                                                          fakeEmitter, fakeTokenProvider, new ObjectMapper());


    @Test
    @SuppressWarnings("unchecked")
    void createPurchaseOrder_savesPendingOrderAndQueuesThePayment_everythingIsOk()
    {
        //ARRANGE
        ClientResponseDTO client = new ClientResponseDTO();
        client.id = "client-1";
        client.userId = "user-1";

        ProductResponseDTO product = new ProductResponseDTO();
        product.id = "p1";
        product.name = "Mouse";
        product.price = new BigDecimal("25.00");

        OrderRequestDTO request = new OrderRequestDTO();
        request.clientId = "client-1";
        request.products = List.of(new OrderProductRecordRequestDTO("p1", 2));

        when(fakeClientsApi.getClientById("Bearer user-token", "client-1")).thenReturn(client);
        when(fakeTokenProvider.getAuthorizationToken()).thenReturn("Bearer service-token");
        when(fakeProductsApi.getProductsByIdsForService("Bearer service-token", List.of("p1"))).thenReturn(List.of(product));

        //MONGO GIVES THE ORDER ITS ID WHEN IT IS SAVED
        doAnswer(invocation -> { ((Order) invocation.getArgument(0)).id = new ObjectId(); return null; })
                .when(fakeRepository).persist(any(Order.class));

        //ACT
        TemporalOrderResponseDTO result = service.createPurchaseOrder(request, "user-token", "ana");

        //ASSERT: THE ORDER IS PENDING, THE CLIENT IS LOOKED UP WITH THE CALLER'S TOKEN
        //AND THE PAYMENT MESSAGE HAS THE CLIENT, THE ORDER AND THE TOTAL (2 x 25 = 50)
        ArgumentCaptor<Message<String>> sent = ArgumentCaptor.forClass(Message.class);
        verify(fakeEmitter).send(sent.capture());
        String message = sent.getValue().getPayload();

        assertAll(() -> assertEquals(OrderState.PENDING, result.orderStatus),
                  () -> verify(fakeClientsApi).getClientById("Bearer user-token", "client-1"),
                  () -> assertTrue(message.contains("\"clientId\":\"client-1\"")),
                  () -> assertTrue(message.contains("\"orderId\":\"" + result.id + "\"")),
                  () -> assertTrue(message.contains("\"amount\":50.00")));
    }

    @Test
    void applyOrderResult_updatesThePendingOrder_resultIsCompleted()
    {
        //ARRANGE
        ObjectId id = new ObjectId();
        when(fakeRepository.applyResult(eq(id), eq(OrderState.COMPLETED), isNull(), eq("orders-service"), any(Instant.class)))
                .thenReturn(1L);

        //ACT
        service.applyOrderResult(new OrderResultMessageDTO(id.toString(), "COMPLETED", null));

        //ASSERT
        verify(fakeRepository).applyResult(eq(id), eq(OrderState.COMPLETED), isNull(), eq("orders-service"), any(Instant.class));
    }

    @Test
    void applyOrderResult_doesNotFail_resultArrivesTwice()
    {
        //ARRANGE: THE ORDER IS NOT PENDING ANYMORE, SO THE ATOMIC UPDATE CHANGES NOTHING
        ObjectId id = new ObjectId();
        when(fakeRepository.applyResult(any(), any(), any(), any(), any())).thenReturn(0L);

        //ACT + ASSERT: THE DUPLICATED MESSAGE IS SIMPLY IGNORED
        assertDoesNotThrow(() -> service.applyOrderResult(new OrderResultMessageDTO(id.toString(), "REJECTED", "No balance")));
    }

    @Test
    void applyOrderResult_ignoresTheMessage_unknownStatus()
    {
        //ACT
        service.applyOrderResult(new OrderResultMessageDTO(new ObjectId().toString(), "BANANA", null));

        //ASSERT
        verify(fakeRepository, never()).applyResult(any(), any(), any(), any(), any());
    }
}
