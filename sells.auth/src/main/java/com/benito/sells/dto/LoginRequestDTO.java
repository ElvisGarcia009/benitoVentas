package com.benito.sells.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDTO {

    @NotBlank(message = "username cannot be empty")
    @Size(max = 30, message = "username cannot be longer than 30 characters")
    public String username;

    @NotBlank(message = "password cannot be empty")
    @Size(max = 72, message = "password cannot be longer than 72 characters")
    public String password;
}