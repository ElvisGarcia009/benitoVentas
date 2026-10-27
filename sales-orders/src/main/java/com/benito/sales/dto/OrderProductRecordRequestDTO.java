package com.benito.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderProductRecordRequestDTO(
        @NotBlank( message = "product ID cannot be empty") String productId,
        @Positive( message = "quantity must be greater than 0") int quantity){}
