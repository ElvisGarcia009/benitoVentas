package com.benito.sells.service;

import com.benito.sells.dto.ProductPaginatedDTO;
import com.benito.sells.entity.Product;
import com.benito.sells.repository.ProductRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProductService {

    private final ProductRepository _productRepository;

    public ProductService(ProductRepository productRepository)
    {
        this._productRepository = productRepository;
    }

    //GET ALL PRODUCTS, PAGINATED
    public ProductPaginatedDTO getAllProducts(int page, int pageSize)
    {
        PanacheQuery<Product> query = _productRepository.findAll().page(page, pageSize);

        List<Product> products = query.stream().toList();
        int totalPages = query.pageCount();
        long totalProducts = query.count();

        return new ProductPaginatedDTO(products, page, pageSize, totalPages, totalProducts);
    }

}
