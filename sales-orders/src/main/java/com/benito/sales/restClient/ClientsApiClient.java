package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "client-api")
@Path("api/v1/client")
public interface ClientsApiClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 500, retryOn = ProcessingException.class)
    ClientResponseDTO getClientById(@HeaderParam("Authorization") String authorization,
                                    @PathParam("id") String id);
}