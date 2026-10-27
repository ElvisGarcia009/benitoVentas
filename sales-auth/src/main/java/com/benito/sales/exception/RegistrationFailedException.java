package com.benito.sales.exception;

public class RegistrationFailedException extends RuntimeException {
    public RegistrationFailedException()
    {
        super("The registration could not be completed, please try again later");
    }
}