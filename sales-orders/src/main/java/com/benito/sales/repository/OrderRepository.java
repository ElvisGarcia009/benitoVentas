package com.benito.sales.repository;

import com.benito.sales.entity.Order;
import com.benito.sales.entity.OrderState;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.time.Instant;

@ApplicationScoped
public class OrderRepository implements PanacheMongoRepository<Order> {

    //CHANGES THE ORDER ONLY IF IT IS STILL PENDING
    public long applyResult(ObjectId orderId, OrderState newState, String rejectionReason, String updatedBy, Instant updatedAt)
    {
        return update("{ '$set': { 'orderStatus': ?1, 'rejectionReason': ?2, 'updatedBy': ?3, 'updatedAt': ?4 } }",
                      newState.name(), rejectionReason, updatedBy, updatedAt)
               .where("_id = ?1 and orderStatus = ?2", orderId, OrderState.PENDING.name());
    }
}
