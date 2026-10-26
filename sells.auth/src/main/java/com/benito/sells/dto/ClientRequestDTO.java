package com.benito.sells.dto;

import java.math.BigDecimal;

public class ClientRequestDTO {

    public String name;
    public String userId;
    public String lastName;
    public String email;
    public String phone;
    public String address;
    public BigDecimal balance;

    public ClientRequestDTO(String name, String userId, String lastName, String email,
                            String phone, String address, BigDecimal balance) {
        this.name = name;
        this.userId = userId;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.balance = balance;
    }

}
