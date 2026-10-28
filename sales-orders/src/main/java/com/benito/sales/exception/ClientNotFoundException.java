package com.benito.sales.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String id)
    {
        super("Client with id: '" + id + "' was not found");
    }
}
