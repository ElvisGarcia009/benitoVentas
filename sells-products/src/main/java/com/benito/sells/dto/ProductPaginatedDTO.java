package com.benito.sells.dto;

import com.benito.sells.entity.Product;

import java.util.List;

public class ProductPaginatedDTO {
    public List<Product> products;

    public int page;
    public int pageSize;
    public int totalPages;
    public long totalProducts;

    public ProductPaginatedDTO(List<Product> products, int page, int pageSize, int totalPages, long  totalProducts)
    {
        this.products = products;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalProducts = totalProducts;
    }
}
