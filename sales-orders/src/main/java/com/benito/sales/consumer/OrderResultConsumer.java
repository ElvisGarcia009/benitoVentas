package com.benito.sales.consumer;

import com.benito.sales.dto.KafkaDTO.OrderResultMessageDTO;
import com.benito.sales.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderResultConsumer {

    //DEPENDENCY INJECTION -> Service & JSON mapper
    private final OrderService _orderService;
    private final ObjectMapper _objectMapper;

    public OrderResultConsumer(OrderService orderService, ObjectMapper objectMapper)
    {
        this._orderService = orderService;
        this._objectMapper = objectMapper;
    }

    @Incoming("order-results-in")
    @Blocking
    public void handleOrderResult(String message) throws JsonProcessingException
    {
        OrderResultMessageDTO result = _objectMapper.readValue(message, OrderResultMessageDTO.class);
        _orderService.applyOrderResult(result);
    }
}
