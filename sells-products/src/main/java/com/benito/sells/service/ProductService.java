package com.benito.sells.service;

import com.benito.sells.dto.ProductRequestDTO;
import com.benito.sells.dto.ProductResponseDTO;
import com.benito.sells.dto.ProductsPaginatedDTO;
import com.benito.sells.entity.Product;
import com.benito.sells.repository.ProductRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;

import java.time.Instant;
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

    //POST A NEW PRODUCT
    public ProductResponseDTO createProduct(ProductRequestDTO requestProduct, String user)
    {
        Product newProduct = new Product();
        newProduct.name = requestProduct.name;
        newProduct.description = requestProduct.description;
        newProduct.quantity = requestProduct.quantity;
        newProduct.price = requestProduct.price;

        //AUDIT FIELDS
        newProduct.status = true;
        newProduct.createdBy = user;
        newProduct.createdAt = Instant.now();

        _productRepository.persist(newProduct);

        return ProductResponseDTO.TurnIntoDTO(newProduct);
    }

    //PUT, UPDATE A PRODUCT
    public ProductResponseDTO updateProduct(ObjectId id, ProductRequestDTO requestProduct, String user)
    {
        Product productFound = _productRepository.findById(id);

        if (productFound == null)
            throw new NotFoundException();

        productFound.name = requestProduct.name;
        productFound.description = requestProduct.description;
        productFound.quantity = requestProduct.quantity;
        productFound.price = requestProduct.price;

        //AUDIT FIELDS
        productFound.updatedBy = user;
        productFound.updatedAt = Instant.now();

        _productRepository.update(productFound);

        return ProductResponseDTO.TurnIntoDTO(productFound);
    }

}
