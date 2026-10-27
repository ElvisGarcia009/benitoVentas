package com.benito.sales.exception;

import org.bson.types.ObjectId;

public class ProductNotFoundException extends RuntimeException{
    public ProductNotFoundException(ObjectId id)
    {
        super("Product with id: '" + id.toString() + "' was not found");
    }
}
