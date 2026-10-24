package com.benito.sells.service;

import com.benito.sells.dto.ProductResponseDTO;
import com.benito.sells.dto.ProductsPaginatedDTO;
import com.benito.sells.entity.Product;
import com.benito.sells.repository.ProductRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class ProductService {

    //DEPENDENCY INJECTION -> Repository
    private final ProductRepository _productRepository;
    public ProductService(ProductRepository productRepository)
    {
        this._productRepository = productRepository;
    }

    //GET ALL PRODUCTS, PAGINATED
    public ProductsPaginatedDTO getAllProducts(int page, int pageSize)
    {
        PanacheQuery<Product> query = _productRepository.findAll().page(page, pageSize);

        List<Product> products = query.stream().toList();
        int totalPages = query.pageCount();
        long totalProducts = query.count();

        return new ProductsPaginatedDTO(products, page, pageSize, totalPages, totalProducts);
    }

    //GET A PRODUCT BY ID
    public ProductResponseDTO getProductById(ObjectId id)
    {
        Product productFound = _productRepository.findById(id);

        if (productFound == null)
            throw new NotFoundException();

        return ProductResponseDTO.TurnIntoDTO(productFound);
    }

}
