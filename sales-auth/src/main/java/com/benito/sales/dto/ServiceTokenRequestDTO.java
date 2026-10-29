package com.benito.sales.dto;

import jakarta.validation.constraints.NotBlank;

public class ServiceTokenRequestDTO {

    @NotBlank
    public String serviceName;

    @NotBlank
    public String secret;

}
