package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "product-api")
@Path("api/v1/product")
public interface ProductsApiClient {

    @GET
    @Path("/internal")
    @Retry(maxRetries = 2, delay = 500, retryOn = ProcessingException.class)
    List<ProductResponseDTO> getProductsByIdsForService(@HeaderParam("Authorization") String authorization,
                                                        @QueryParam("ids") List<String> ids);
}