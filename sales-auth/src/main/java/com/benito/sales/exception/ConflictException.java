package com.benito.sales.exception;

public class ConflictException extends RuntimeException {

    public ConflictException()
    {
        super("Username or email is not available");
    }

}
