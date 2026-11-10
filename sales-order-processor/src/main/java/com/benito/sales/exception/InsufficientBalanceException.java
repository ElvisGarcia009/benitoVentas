package com.benito.sales.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException()
    {
        super("The client does not have enough balance to complete this purchase");
    }
}
