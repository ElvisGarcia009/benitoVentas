package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import com.benito.sales.dto.restClientDTO.ClientUpdateRequestDTO;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "client-api")
@Path("api/v1/client")

public interface ClientsApiClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    ClientResponseDTO getClientById(@HeaderParam("Authorization") String authorization,
                                    @PathParam("id") String id);


    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    ClientResponseDTO updateClient(@HeaderParam("Authorization") String authorization,
                                          @PathParam("id") String id,
                                          @Valid ClientUpdateRequestDTO requestClient);



}