package com.benito.sales.dto;

import com.benito.sales.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponseDTO {

    public String id;
    public String clientId;
    public List<OrderProductRecordResponseDTO> products;
    public BigDecimal orderTotal;
    public Instant createdAt;

    public OrderResponseDTO(String id, String clientId, List<OrderProductRecordResponseDTO> products,
                            BigDecimal orderTotal, Instant createdAt)
    {
        this.id = id;
        this.clientId = clientId;
        this.products = products;
        this.orderTotal = orderTotal;
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
                order.createdAt);
    }
}