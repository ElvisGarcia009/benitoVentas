package com.benito.sales.consumer;

import com.benito.sales.dto.OrderMessageDTO;
import com.benito.sales.service.OrderProcessorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderProcessor {

    //DEPENDENCY INJECTION -> Service & JSON mapper
    private final OrderProcessorService _processorService;
    private final ObjectMapper _objectMapper;

    public OrderProcessor(OrderProcessorService processorService, ObjectMapper objectMapper)
    {
        this._processorService = processorService;
        this._objectMapper = objectMapper;
    }

    //THE MESSAGE HAS EVERYTHING NEEDED TO CHARGE THE CLIENT: CLIENT ID, ORDER ID AND AMOUNT
    @Incoming("orders-in")
    @Blocking
    public void handleOrderRequest(String message) throws JsonProcessingException
    {
        OrderMessageDTO order = _objectMapper.readValue(message, OrderMessageDTO.class);
        _processorService.processPayment(order);
    }
}
