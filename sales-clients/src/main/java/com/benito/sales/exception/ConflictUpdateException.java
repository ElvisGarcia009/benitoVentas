package com.benito.sales.exception;

public class ConflictUpdateException extends RuntimeException {
    public ConflictUpdateException() {
        super("The debit was not allowed because of concurrent modifications, please retry");
    }
}
