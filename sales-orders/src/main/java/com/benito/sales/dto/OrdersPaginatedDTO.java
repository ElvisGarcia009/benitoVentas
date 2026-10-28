package com.benito.sales.dto;

import java.util.List;

public class OrdersPaginatedDTO {

    public List<OrderResponseDTO> orders;

    public int page;
    public int pageSize;
    public int totalPages;
    public long totalOrders;

    public OrdersPaginatedDTO(List<OrderResponseDTO> orders, int page, int pageSize, int totalPages, long totalOrders)
    {
        this.orders = orders;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalOrders = totalOrders;
    }
}