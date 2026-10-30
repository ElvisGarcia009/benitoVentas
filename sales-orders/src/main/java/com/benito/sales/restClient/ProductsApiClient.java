package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "product-api")
@Path("api/v1/product")
public interface ProductsApiClient {

    @GET
    @Path("/internal/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 500, retryOn = ProcessingException.class)
    ProductResponseDTO getProductById(@HeaderParam("Authorization") String authorization,
                                      @PathParam("id") String id);
}