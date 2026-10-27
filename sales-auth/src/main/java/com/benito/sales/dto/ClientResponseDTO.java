package com.benito.sales.dto;

import java.math.BigDecimal;

public class ClientResponseDTO {

    public String id;
    public String userId;
    public String name;
    public String lastName;
    public String email;
    public String phone;
    public String address;
    public BigDecimal balance;


    public ClientResponseDTO(String id, String userId, String name, String lastName,
                             String email, String phone, String address, BigDecimal balance)
    {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.balance = balance;
    }
}
