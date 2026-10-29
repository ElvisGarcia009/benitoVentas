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
    public String userId;
    public List<OrderProductRecord> products;
    public BigDecimal orderTotal;
    public OrderState orderStatus;
    public String rejectionReason;   //WHY THE ORDER WAS REJECTED (NULL IF IT WASN'T)


    //AUDIT FIELDS
    public boolean status;
    public String createdBy;
    public Instant createdAt;
    public String updatedBy;
    public Instant updatedAt;

}