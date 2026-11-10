package com.benito.sales.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@MongoEntity(collection = "clients")
public class Client {

    //MAIN FIELDS
    public ObjectId id;
    public String userId; // REFERENCE TO USER DOCUMENT
    public String name;
    public String lastName;
    public String phone;
    public String address;
    public BigDecimal balance;
    public List<String> processedOrders;  // ORDERS ALREADY CHARGED (IDEMPOTENCY LOG OF THE DEBITS)

    //AUDIT FIELDS
    public boolean status;
    public String createdBy;
    public Instant createdAt;
    public String updatedBy;
    public Instant updatedAt;
}
