package com.benito.sales.entity;


import java.math.BigDecimal;

public record OrderProductRecord(String productId,
                                 String productName,
                                 BigDecimal pricePerUnit,
                                 int quantity,
                                 BigDecimal total) { }
