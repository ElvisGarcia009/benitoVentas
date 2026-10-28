package com.benito.sales.exception;

public class OrderAccessDeniedException extends RuntimeException {
    public OrderAccessDeniedException()
    {
        super("You can only access your own orders");
    }
}
