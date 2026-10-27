package com.benito.sales.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public class UserRequestDTO {

    @NotBlank(message = "username cannot be empty")
    @Size(max = 30, message = "username cannot be longer than 30 characters")
    public String username;

    @NotBlank(message = "email cannot be empty")
    @Email(message = "email is not valid")
    @Size(max = 100, message = "email cannot be longer than 100 characters")
    public String email;

    @NotBlank(message = "password cannot be empty")
    @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
            message = "password must contain at least one lowercase letter, one uppercase letter, one number and one special character")
    public String password;

    @NotEmpty(message = "roles cannot be empty")
    public Set<@Pattern(regexp = "admin|client", message = "role must be 'admin' or 'client'") String> roles;
}