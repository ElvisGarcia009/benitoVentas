package com.benito.sales.exception;

import org.bson.types.ObjectId;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(ObjectId id)
    {
        super("Order with id: '" + id.toString() + "' was not found");
    }
}
