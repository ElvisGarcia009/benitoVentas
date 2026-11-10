package com.benito.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public class OrderRequestDTO {

    @NotBlank(message = "client ID must be provided")
    public String clientId;

    @NotEmpty(message = "you need at least 1 product to make an order")
    @Size(max = 30, message = "you can't buy more than 30 different items")
    public List<@Valid OrderProductRecordRequestDTO> products;

    @AssertTrue(message = "Products cannot contain duplicate IDs")
    public boolean isProductIdsUnique()
    {
        if (products == null)
            return true;

        return products.stream()
                .map(OrderProductRecordRequestDTO::productId)
                .distinct()
                .count() == products.size();
    }
}
