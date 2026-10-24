package com.benito.sells.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ProductRequestDTO {

    @NotBlank(message = "name cannot be empty")
    @Max(value = 30, message = "name cannot be longer than 30 characters")
    public String name;

    @NotBlank(message = "description cannot be empty")
    public String description;

    @Positive(message = "quantity cannot be negative")
    @NotNull(message = "quantity cannot be null")
    public int quantity;

    @Positive(message = "price cannot be negative")
    @NotNull(message = "price cannot be null")
    public BigDecimal price;
}
