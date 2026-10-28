package com.benito.sales.exception;

public class ClientAccessDeniedException extends RuntimeException {
    public ClientAccessDeniedException()
    {
        super("You can only make orders for your own client account");
    }
}
