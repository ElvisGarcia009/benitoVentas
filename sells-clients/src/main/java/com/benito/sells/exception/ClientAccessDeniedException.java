package com.benito.sells.exception;

public class ClientAccessDeniedException extends RuntimeException {
    public ClientAccessDeniedException()
    {
        super("You are not authorized to access this client's information");
    }
}
