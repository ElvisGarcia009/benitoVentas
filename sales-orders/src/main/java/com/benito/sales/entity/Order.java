package com.benito.sales.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@MongoEntity(collection = "orders")
public class Order {

    //MAIN FIELDS
    public ObjectId id;
    public String clientId;
    public List<OrderProductRecord> products;
    public BigDecimal orderTotal;

    //AUDIT FIELDS
    public boolean status;
    public String createdBy;
    public Instant createdAt;
    public String updatedBy;
    public Instant updatedAt;

}
