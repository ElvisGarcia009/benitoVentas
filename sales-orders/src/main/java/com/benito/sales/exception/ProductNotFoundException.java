package com.benito.sales.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message)
    {
        super(message);
    }
}
