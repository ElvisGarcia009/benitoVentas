package com.benito.sales.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;

//ONE DOCUMENT PER ORDER
@MongoEntity(collection = "payments")
public class OrdersPayments {

    //MAIN FIELDS
    public ObjectId id;
    public String orderId;          //UNIQUE: IDEMPOTENCY KEY
    public String clientId;
    public BigDecimal amount;
    public OrderStatus status;
    public String rejectionReason;

    //AUDIT FIELDS
    public Instant createdAt;
    public Instant updatedAt;
}
