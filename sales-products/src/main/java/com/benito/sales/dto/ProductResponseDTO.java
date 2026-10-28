package com.benito.sales.dto;

import com.benito.sales.entity.Product;

import java.math.BigDecimal;

public class ProductResponseDTO {

    public String id;
    public String name;
    public String description;
    public BigDecimal price;

    public ProductResponseDTO(String id, String name, String description, BigDecimal price)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public static ProductResponseDTO TurnIntoDTO(Product product)
    {
        return new ProductResponseDTO(product.id.toString(),
                                    product.name,
                                    product.description,
                                    product.price);
    }

}
