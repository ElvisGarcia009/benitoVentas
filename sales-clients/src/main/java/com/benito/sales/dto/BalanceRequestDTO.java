package com.benito.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BalanceRequestDTO {

    @NotNull(message = "amount cannot be null")
    @Positive(message = "amount must be greater than 0")
    public BigDecimal amount;
}