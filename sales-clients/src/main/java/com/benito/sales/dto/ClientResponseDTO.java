package com.benito.sales.dto;

import com.benito.sales.entity.Client;

import java.math.BigDecimal;

public class ClientResponseDTO {

    public String id;
    public String userId;
    public String name;
    public String lastName;
    public String phone;
    public String address;
    public BigDecimal balance;

    public ClientResponseDTO(String id, String userId, String name, String lastName,
                             String phone, String address, BigDecimal balance)
    {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.balance = balance;
    }

    public static ClientResponseDTO TurnIntoDTO(Client client)
    {
        return new ClientResponseDTO(client.id.toString(),
                                    client.userId,
                                    client.name,
                                    client.lastName,
                                    client.phone,
                                    client.address,
                                    client.balance);
    }

}
