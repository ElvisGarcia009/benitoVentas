package com.benito.sales.dto;

import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponseDTO {

    public String id;
    public String clientId;
    public List<OrderProductRecordResponseDTO> products;
    public BigDecimal orderTotal;
    public OrderState orderStatus;
    public String rejectionReason;
    public Instant createdAt;

    public OrderResponseDTO(String id, String clientId, List<OrderProductRecordResponseDTO> products,
                            BigDecimal orderTotal, OrderState orderStatus, String rejectionReason, Instant createdAt)
    {
        this.id = id;
        this.clientId = clientId;
        this.products = products;
        this.orderTotal = orderTotal;
        this.orderStatus = orderStatus;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
    }

    public static OrderResponseDTO TurnIntoDTO(Order order)
    {
        List<OrderProductRecordResponseDTO> products = order.products.stream()
                .map(product -> new OrderProductRecordResponseDTO(product.productId(),
                                                                                    product.productName(),
                                                                                    product.pricePerUnit(),
                                                                                    product.quantity(),
                                                                                    product.total()))
                .toList();

        return new OrderResponseDTO(order.id.toString(),
                order.clientId,
                products,
                order.orderTotal,
                order.orderStatus,
                order.rejectionReason,
                order.createdAt);
    }
}