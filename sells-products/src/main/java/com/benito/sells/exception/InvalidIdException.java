package com.benito.sells.exception;

public class InvalidIdException extends RuntimeException {

    public InvalidIdException(String id)
    {
        super("The provided Id: '" + id +  "' is not valid");
    }

}
