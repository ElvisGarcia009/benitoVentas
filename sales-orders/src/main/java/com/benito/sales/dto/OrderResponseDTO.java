package com.benito.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderResponseDTO {
    public String id;
    public String clientId;
    public List<OrderProductRecordResponseDTO> products;
    public BigDecimal orderTotal;
}
