package com.benito.sells.exception;

public class SelfModificationException extends RuntimeException {
    public SelfModificationException()
    {
        super("An admin cannot change his own roles or deactivate his own account");
    }
}