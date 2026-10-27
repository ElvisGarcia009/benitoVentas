package com.benito.sales.dto.restClientDTO;

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
}
