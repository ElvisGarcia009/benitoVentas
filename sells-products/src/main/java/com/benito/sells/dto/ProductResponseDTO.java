package com.benito.sells.dto;

import com.benito.sells.entity.Product;
import org.bson.types.ObjectId;

import java.math.BigDecimal;

public class ProductResponseDTO {

    public String id;
    public String name;
    public String description;
    public int quantity;
    public BigDecimal price;

    public ProductResponseDTO(String id, String name, String description, int quantity, BigDecimal price)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
    }

    public static ProductResponseDTO TurnIntoDTO(Product product)
    {
        return new ProductResponseDTO(product.id.toString(),
                                    product.name,
                                    product.description,
                                    product.quantity,
                                    product.price);
    }

}
