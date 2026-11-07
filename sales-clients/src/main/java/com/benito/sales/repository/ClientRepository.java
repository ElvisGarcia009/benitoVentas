package com.benito.sales.repository;

import com.benito.sales.entity.Client;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;

@ApplicationScoped
public class ClientRepository implements PanacheMongoRepository<Client> {

    public Client debitAtomicUpdate(Client client, BigDecimal amount)
    {
        return mongoCollection().findOneAndUpdate(

                Filters.and(
                        Filters.eq("_id", client.id),
                        Filters.eq("status", true),
                        Filters.gte("balance", amount)
                ),
                Updates.combine(
                        Updates.inc("balance", amount.negate()),
                        Updates.set("updatedBy", client.updatedBy),
                        Updates.set("updatedAt", client.updatedAt)
                ),

                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
    }

    public Client creditAtomicUpdate(Client client, BigDecimal amount)
    {
        return mongoCollection().findOneAndUpdate(
                Filters.and(
                        Filters.eq("_id", client.id),
                        Filters.eq("status", true)
                ),
                Updates.combine(
                        Updates.inc("balance", amount),
                        Updates.set("updatedBy", client.updatedBy),
                        Updates.set("updatedAt", client.updatedAt)
                ),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
    }

    public long fieldsAtomicUpdate(Client client)
    {
        return update("{ '$set' : {'name' : ?1, 'lastName' : ?2, 'phone' : ?3, 'address' : ?4, 'updatedBy' : ?5, 'updatedAt' : ?6 } }",
                            client.name, client.lastName, client.phone, client.address, client.updatedBy, client.updatedAt)
               .where("_id = ?1 and status = ?2", client.id , true);
    }

    public long deleteAtomicUpdate(Client client)
    {
        return update("{ '$set' : {'status' : ?1, 'updatedBy' : ?2, 'updatedAt' : ?3 } }",
                false, client.updatedBy, client.updatedAt)
                .where("_id = ?1 and status = ?2", client.id, true);
    }
}
