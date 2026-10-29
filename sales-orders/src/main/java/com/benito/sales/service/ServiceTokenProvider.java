package com.benito.sales.service;

import com.benito.sales.dto.restClientDTO.ServiceTokenRequestDTO;
import com.benito.sales.dto.restClientDTO.TokenResponseDTO;
import com.benito.sales.restClient.AuthApiClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

//ASKS THE AUTH SERVICE FOR A SHORT-LIVED TOKEN THAT IDENTIFIES THIS SERVICE
@ApplicationScoped
public class ServiceTokenProvider {

    private final AuthApiClient _authApi;
    private final String _serviceSecret;

    public ServiceTokenProvider(@RestClient AuthApiClient authApi,
                                @ConfigProperty(name = "orders.service.secret") String serviceSecret)
    {
        this._authApi = authApi;
        this._serviceSecret = serviceSecret;
    }

    public String getAuthorizationToken()
    {
        TokenResponseDTO token = _authApi.getServiceToken(
                       new ServiceTokenRequestDTO("orders-service", _serviceSecret));

        return "Bearer " + token.accessToken;
    }
}