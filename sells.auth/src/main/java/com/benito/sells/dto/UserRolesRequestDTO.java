package com.benito.sells.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public class UserRolesRequestDTO {

    @NotEmpty(message = "roles cannot be empty")
    public Set<@Pattern(regexp = "admin|client", message = "role must be 'admin' or 'client'") String> roles;

}