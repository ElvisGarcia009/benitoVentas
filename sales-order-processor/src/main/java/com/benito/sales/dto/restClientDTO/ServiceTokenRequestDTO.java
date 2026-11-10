package com.benito.sales.dto.restClientDTO;

public class ServiceTokenRequestDTO {

    public String serviceName;
    public String secret;

    public ServiceTokenRequestDTO(String serviceName, String secret)
    {
        this.serviceName = serviceName;
        this.secret = secret;
    }
}
