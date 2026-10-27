package com.benito.sales.dto;

import jakarta.validation.constraints.*;
import jakarta.ws.rs.DefaultValue;

import java.math.BigDecimal;

public class ClientRequestDTO {

    @NotBlank(message = "name cannot be empty")
    @Size(max = 50, message = "name cannot be longer than 50 characters")
    public String name;

    @NotBlank(message = "userID cannot be empty")
    public String userId;

    @NotBlank(message = "lastName cannot be empty")
    @Size(max = 50, message = "lastName cannot be longer than 50 characters")
    public String lastName;

    @NotBlank(message = "email cannot be empty")
    @Email(message = "email is not valid")
    @Size(max = 100, message = "email cannot be longer than 100 characters")
    public String email;

    @Size(max = 20, message = "phone cannot be longer than 20 characters")
    public String phone;

    @Size(max = 200, message = "address cannot be longer than 200 characters")
    public String address;

//    @PositiveOrZero(message = "balance cannot be negative")
//    public BigDecimal balance;
    //NOTE: FOR NOW, ONLY ADMINS CAN GRANT CLIENTS NEW BALANCES (A WAY OF CONTROL)
}
