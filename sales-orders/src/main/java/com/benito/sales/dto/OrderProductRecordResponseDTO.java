package com.benito.sales.dto;

import java.math.BigDecimal;

public record OrderProductRecordResponseDTO(String productId,
                                            String productName,
                                            BigDecimal pricePerUnit,
                                            int quantity,
                                            BigDecimal total) { }
