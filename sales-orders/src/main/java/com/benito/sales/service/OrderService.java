package com.benito.sales.service;

import com.benito.sales.dto.OrderProductRecordRequestDTO;
import com.benito.sales.dto.OrderResponseDTO;
import com.benito.sales.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderService {

    private final OrderRepository _orderRepository;

    public OrderService(OrderRepository orderRepository)
    {
        this._orderRepository = orderRepository;
    }

    //CREATE A PURCHASE ORDER
//    public OrderResponseDTO createPurchaseOrder(OrderProductRecordRequestDTO requestOrder)
//    {
//
//    }


}
