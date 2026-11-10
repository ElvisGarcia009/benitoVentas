package com.benito.sales.repository;

import com.benito.sales.entity.OrdersPayments;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrdersPaymentsRepository implements PanacheMongoRepository<OrdersPayments> {

    private final static Logger LOG = Logger.getLogger(OrdersPaymentsRepository.class);

    public OrdersPayments findByOrderId(String orderId)
    {
        return find("orderId", orderId).firstResult();
    }


    //THE DATABASE GUARANTEES THERE IS ONLY ONE PAYMENT DOCUMENT PER ORDER
    void createIndexes(@Observes StartupEvent event)
    {
        try
        {
            mongoCollection().createIndex(Indexes.ascending("orderId"), new IndexOptions().unique(true));
        }
        catch (RuntimeException exception)
        {
            LOG.error("The unique index on payments.orderId could not be created", exception);
        }
    }
}
