package com.benito.sells.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {

    @NotBlank(message = "username cannot be empty")
    @Size(max = 30, message = "username cannot be longer than 30 characters")
    public String username;

    @NotBlank(message = "name cannot be empty")
    @Size(max = 50, message = "name cannot be longer than 50 characters")
    public String name;

    @NotBlank(message = "lastName cannot be empty")
    @Size(max = 50, message = "lastName cannot be longer than 50 characters")
    public String lastName;

    @NotBlank(message = "email cannot be empty")
    @Email(message = "email is not valid")
    @Size(max = 100, message = "email cannot be longer than 100 characters")
    public String email;

    @NotBlank(message = "password cannot be empty")
    @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).*$",
            message = "password must contain at least one lowercase letter, one uppercase letter, one number and one special character")
    public String password;
}