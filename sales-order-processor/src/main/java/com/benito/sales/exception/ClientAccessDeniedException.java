package com.benito.sales.exception;

public class ClientAccessDeniedException extends RuntimeException {
    public ClientAccessDeniedException()
    {
        super("This service is not allowed to charge this client");
    }
}
