package com.benito.sales.consumer;

import com.benito.sales.service.OrderService;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderConsumer {

    //DEPENDENCY INJECTION -> Service
    private final OrderService _orderService;

    public OrderConsumer(OrderService orderService)
    {
        this._orderService = orderService;
    }

    //THE MESSAGE ONLY POINTS WHICH ORDER SHALL BE PROCESS. THE ORDER ITSELF IS READ FROM MONGO
    @Incoming("orders-in")
    @Blocking
    public void handleOrderRequest(String orderId)
    {
        _orderService.processOrder(new ObjectId(orderId));
    }
}