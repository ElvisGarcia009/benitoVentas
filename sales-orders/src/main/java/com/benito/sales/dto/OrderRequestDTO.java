package com.benito.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderRequestDTO {

    @NotBlank(message = "client ID must be provided")
    public String clientId;

    @NotEmpty(message = "you need at least 1 product to make an order")
    public List<@Valid OrderProductRecordRequestDTO> products;
}