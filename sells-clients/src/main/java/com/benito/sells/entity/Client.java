package com.benito.sells.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.Instant;

@MongoEntity(collection = "clients")
public class Client {

    //MAIN FIELDS
    public ObjectId id;
    public String username;
    public String name;
    public String lastName;
    public String email;
    public String phone;
    public String address;
    public BigDecimal balance;

    //AUDIT FIELDS
    public boolean status;
    public String createdBy;
    public Instant createdAt;
    public String updatedBy;
    public Instant updatedAt;
}
