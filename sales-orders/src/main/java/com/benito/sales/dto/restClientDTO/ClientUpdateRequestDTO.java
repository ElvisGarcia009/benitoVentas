package com.benito.sales.dto.restClientDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ClientUpdateRequestDTO {

    @NotBlank(message = "name cannot be empty")
    @Size(max = 50, message = "name cannot be longer than 50 characters")
    public String name;

    @NotBlank(message = "lastName cannot be empty")
    @Size(max = 50, message = "lastName cannot be longer than 50 characters")
    public String lastName;

    @Size(max = 20, message = "phone cannot be longer than 20 characters")
    public String phone;

    @Size(max = 200, message = "address cannot be longer than 200 characters")
    public String address;

    @PositiveOrZero(message = "balance cannot be negative")
    public BigDecimal balance;
}
