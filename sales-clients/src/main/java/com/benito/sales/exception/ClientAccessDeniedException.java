package com.benito.sales.exception;

public class ClientAccessDeniedException extends RuntimeException {
    public ClientAccessDeniedException()
    {
        super("You are not authorized to access this client's information");
    }
}
