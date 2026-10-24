package com.benito.sells.repository;

import com.benito.sells.entity.Product;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheMongoRepository<Product> {
}
