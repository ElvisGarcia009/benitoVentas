package com.benito.sells.dto;

import com.benito.sells.entity.Client;

import java.util.List;

public class ClientsPaginatedDTO {
    public List<Client> clients;

    public int page;
    public int pageSize;
    public int totalPages;
    public long totalClients;

    public ClientsPaginatedDTO(List<Client> clients, int page, int pageSize, int totalPages, long  totalClients)
    {
        this.clients = clients;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalClients = totalClients;
    }
}
