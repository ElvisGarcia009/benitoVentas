package com.benito.sales.repository;

import com.benito.sales.entity.Client;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientRepository implements PanacheMongoRepository<Client> {
}
