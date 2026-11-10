package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.BalanceRequestDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "client-api")
@Path("api/v1/client")
public interface ClientsApiClient {

    //CLIENTS USES THE ORDER ID AS IDEMPOTENCY KEY AND NEVER CHARGES THE SAME ORDER TWICE
    @POST
    @Path("/{id}/balance/debit")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 500, retryOn = ProcessingException.class)
    void debitBalance(@HeaderParam("Authorization") String authorization,
                      @PathParam("id") String id,
                      BalanceRequestDTO requestBalance);
}
