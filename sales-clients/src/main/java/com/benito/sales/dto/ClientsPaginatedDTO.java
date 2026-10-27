package com.benito.sales.dto;
import java.util.List;

public class ClientsPaginatedDTO {
    public List<ClientResponseDTO> clients;

    public int page;
    public int pageSize;
    public int totalPages;
    public long totalClients;

    public ClientsPaginatedDTO(List<ClientResponseDTO> clients, int page, int pageSize, int totalPages, long  totalClients)
    {
        this.clients = clients;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalClients = totalClients;
    }
}
