package com.benito.sales.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductRequestDTO {

    @NotBlank(message = "name cannot be empty")
    @Size(max = 100, message = "name cannot be longer than 100 characters")
    public String name;

    @NotBlank(message = "description cannot be empty")
    @Size(max = 30, message = "name cannot be longer than 500 characters")
    public String description;

    @PositiveOrZero(message = "quantity cannot be negative")
    @NotNull(message = "quantity cannot be null")
    public int quantity;

    @PositiveOrZero(message = "price cannot be negative")
    @NotNull(message = "price cannot be null")
    public BigDecimal price;
}
