package com.benito.sells.repository;

import com.benito.sells.entity.Client;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientRepository implements PanacheMongoRepository<Client> {
}
