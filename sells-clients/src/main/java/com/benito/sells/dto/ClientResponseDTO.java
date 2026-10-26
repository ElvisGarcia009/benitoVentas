package com.benito.sells.dto;

import com.benito.sells.entity.Client;

import java.math.BigDecimal;

public class ClientResponseDTO {

    public String id;
    public String username;
    public String name;
    public String lastName;
    public String email;
    public String phone;
    public String address;
    public BigDecimal balance;

    public ClientResponseDTO(String id, String username, String name, String lastName,
                             String email, String phone, String address, BigDecimal balance)
    {
        this.id = id;
        this.username = username;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.balance = balance;
    }

    public static ClientResponseDTO TurnIntoDTO(Client client)
    {
        return new ClientResponseDTO(client.id.toString(),
                                    client.username,
                                    client.name,
                                    client.lastName,
                                    client.email,
                                    client.phone,
                                    client.address,
                                    client.balance);
    }

}
