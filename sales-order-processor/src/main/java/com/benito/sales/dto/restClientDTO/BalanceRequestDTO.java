package com.benito.sales.dto.restClientDTO;

import java.math.BigDecimal;

public class BalanceRequestDTO {

    public BigDecimal amount;
    public String orderId;

    public BalanceRequestDTO(BigDecimal amount, String orderId)
    {
        this.amount = amount;
        this.orderId = orderId;
    }
}
