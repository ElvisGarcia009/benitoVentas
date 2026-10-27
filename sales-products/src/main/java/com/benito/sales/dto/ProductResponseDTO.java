package com.benito.sales.dto;

import com.benito.sales.entity.Product;

import java.math.BigDecimal;

public class ProductResponseDTO {

    public String id;
    public String name;
    public String description;
    public int stock;
    public BigDecimal price;

    public ProductResponseDTO(String id, String name, String description, int stock, BigDecimal price)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.stock = stock;
        this.price = price;
    }

    public static ProductResponseDTO TurnIntoDTO(Product product)
    {
        return new ProductResponseDTO(product.id.toString(),
                                    product.name,
                                    product.description,
                                    product.stock,
                                    product.price);
    }

}
