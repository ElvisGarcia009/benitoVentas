package com.benito.sales.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Set;

@MongoEntity(collection = "users")
public class User {

    //MAIN FIELDS
    public ObjectId id;
    public String username;
    public String email;
    public String passwordHash;
    public Set<String> roles;

    //AUDIT FIELDS
    public boolean status;
    public String createdBy;
    public Instant createdAt;
    public String updatedBy;
    public Instant updatedAt;
}
