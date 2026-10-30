package com.benito.sales.exception;

public class InvalidServiceCredentialsException extends RuntimeException {
    public InvalidServiceCredentialsException()
    {
        super("Invalid service credentials");
    }
}
