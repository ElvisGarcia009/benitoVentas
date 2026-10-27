package com.benito.sales.exception;

import org.bson.types.ObjectId;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(ObjectId id)
    {
        super("User with id: '" + id.toString() + "' was not found");
    }
}