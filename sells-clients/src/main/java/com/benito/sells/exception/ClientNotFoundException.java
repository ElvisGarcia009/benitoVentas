package com.benito.sells.exception;

import org.bson.types.ObjectId;

public class ClientNotFoundException extends RuntimeException{
    public ClientNotFoundException(ObjectId id)
    {
        super("Client with id: '" + id.toString() + "' was not found");
    }
}
