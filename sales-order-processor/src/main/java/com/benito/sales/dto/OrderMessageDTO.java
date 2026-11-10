package com.benito.sales.dto;

import java.math.BigDecimal;

public class OrderMessageDTO {

    public String clientId;
    public String orderId;
    public BigDecimal amount;

    public OrderMessageDTO() { }

    public OrderMessageDTO(String clientId, String orderId, BigDecimal amount)
    {
        this.clientId = clientId;
        this.orderId = orderId;
        this.amount = amount;
    }
}
