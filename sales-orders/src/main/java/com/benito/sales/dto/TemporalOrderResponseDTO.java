package com.benito.sales.dto;

import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderProductRecord;
import com.benito.sales.entity.OrderState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TemporalOrderResponseDTO {

    public String id;
    public String clientId;
    public List<TemporalOrderProductRecordResponseDTO> products;
    public OrderState orderStatus;
    public Instant createdAt;

    public TemporalOrderResponseDTO(String id,
                                    String clientId,
                                    List<TemporalOrderProductRecordResponseDTO> products,
                                    OrderState orderStatus,
                                    Instant createdAt)
    {
        this.id = id;
        this.clientId = clientId;
        this.products = products;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
    }

    public static TemporalOrderResponseDTO TurnIntoDTO(Order order)
    {
        List<TemporalOrderProductRecordResponseDTO> products = new ArrayList<>();

        for (OrderProductRecord product : order.products)
        {
            products.add(new TemporalOrderProductRecordResponseDTO(product.productId(),
                    product.quantity()));
        }

        return new TemporalOrderResponseDTO(order.id.toString(),
                order.clientId,
                products,
                order.orderStatus,
                order.createdAt);
    }
}