package com.benito.sells.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;
import java.math.BigDecimal;
import java.time.Instant;

@MongoEntity(collection = "products")
public class Product {

    //MAIN FIELDS
    public ObjectId id;
    public String name;
    public String description;
    public int quantity;
    public BigDecimal price;


    //AUDIT FIELDS
    public boolean status;
    public String createdBy;
    public Instant createdAt;
    public String updatedBy;
    public Instant updatedAt;
}
