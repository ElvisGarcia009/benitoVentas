package com.benito.sales.dto;

import java.util.List;

public class UsersPaginatedDTO {

    public List<UserResponseDTO> users;

    public int page;
    public int pageSize;
    public int totalPages;
    public long totalUsers;

    public UsersPaginatedDTO(List<UserResponseDTO> users, int page, int pageSize, int totalPages, long totalUsers)
    {
        this.users = users;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.totalUsers = totalUsers;
    }
}