package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ProductRequestDTO;
import com.benito.sales.dto.restClientDTO.ProductResponseDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient(configKey = "product-api")
@Path("api/v1/product")
public interface ProductsApiClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    ProductResponseDTO getProductById(@HeaderParam("Authorization") String authorization,
                                      @PathParam("id") String id);

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ProductResponseDTO updateProduct(@HeaderParam("Authorization") String authorization,
                                            @PathParam("id") String id,
                                            @Valid ProductRequestDTO requestProduct);

}
