package com.benito.sells.service;

import com.benito.sells.dto.ProductRequestDTO;
import com.benito.sells.dto.ProductResponseDTO;
import com.benito.sells.dto.ProductsPaginatedDTO;
import com.benito.sells.entity.Product;
import com.benito.sells.exception.ProductNotFoundException;
import com.benito.sells.repository.ProductRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
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
        PanacheQuery<Product> query = _productRepository.findAll(Sort.by("name").ascending()).page(page, pageSize);

        List<Product> products = query.stream().filter(product -> product.status).toList();
        int totalPages = query.pageCount();
        long totalProducts = query.count();

        return new ProductsPaginatedDTO(products, page, pageSize, totalPages, totalProducts);
    }



    //GET A PRODUCT BY ID
    public ProductResponseDTO getProductById(ObjectId id)
    {
        Product productFound = findValidProduct(id);
        return ProductResponseDTO.TurnIntoDTO(productFound);
    }



    //POST A NEW PRODUCT
    public ProductResponseDTO createProduct(ProductRequestDTO requestProduct, String username)
    {
        Product newProduct = new Product();
        newProduct.name = requestProduct.name;
        newProduct.description = requestProduct.description;
        newProduct.quantity = requestProduct.quantity;
        newProduct.price = requestProduct.price;

        //AUDIT FIELDS
        newProduct.status = true;
        newProduct.createdBy = username;
        newProduct.createdAt = Instant.now();

        _productRepository.persist(newProduct);

        return ProductResponseDTO.TurnIntoDTO(newProduct);
    }



    //PUT, UPDATE A PRODUCT
    public ProductResponseDTO updateProduct(ObjectId id, ProductRequestDTO requestProduct, String username)
    {
        Product productFound = findValidProduct(id);

        productFound.name = requestProduct.name;
        productFound.description = requestProduct.description;
        productFound.quantity = requestProduct.quantity;
        productFound.price = requestProduct.price;

        //AUDIT FIELDS
        productFound.updatedBy = username;
        productFound.updatedAt = Instant.now();

        _productRepository.update(productFound);

        return ProductResponseDTO.TurnIntoDTO(productFound);
    }



    //DELETE A PRODUCT (this method just deactivate the product)
    public void deleteProduct(ObjectId id, String username)
    {
        Product productFound = findValidProduct(id);

        productFound.status = false;
        productFound.updatedBy = username;
        productFound.updatedAt = Instant.now();

        _productRepository.update(productFound);

    }



    //HELPER
    public Product findValidProduct(ObjectId id)
    {
        Product productContainer = _productRepository.findById(id);

        if (productContainer == null || !productContainer.status)
            throw new ProductNotFoundException(id);

        return productContainer;
    }
}
