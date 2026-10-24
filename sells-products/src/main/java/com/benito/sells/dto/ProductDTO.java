package com.benito.sells.dto;

import com.benito.sells.entity.Product;
import org.bson.types.ObjectId;

import java.math.BigDecimal;

public class ProductDTO {

    public ObjectId id;
    public String name;
    public String description;
    public int quantity;
    public BigDecimal price;

    public ProductDTO(ObjectId id, String name, String description, int quantity, BigDecimal price)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
    }

    public static ProductDTO TurnIntoDTO(Product product)
    {
        return new ProductDTO(product.id,
                product.name,
                product.description,
                product.quantity,
                product.price);
    }

}
