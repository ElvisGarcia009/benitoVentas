package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ServiceTokenRequestDTO;
import com.benito.sales.dto.restClientDTO.TokenResponseDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "auth-api")
@Path("api/v1/auth")
public interface AuthApiClient {

    @POST
    @Path("/service-token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 500, retryOn = ProcessingException.class)
    TokenResponseDTO getServiceToken(ServiceTokenRequestDTO request);
}