package com.benito.sales.dto.restClientDTO;

import java.math.BigDecimal;

public class BalanceRequestDTO {

    public BigDecimal amount;

    public BalanceRequestDTO(BigDecimal amount)
    {
        this.amount = amount;
    }
}