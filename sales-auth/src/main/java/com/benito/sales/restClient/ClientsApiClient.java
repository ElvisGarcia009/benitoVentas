package com.benito.sales.restClient;

import com.benito.sales.dto.restClientDTO.ClientRequestDTO;
import com.benito.sales.dto.restClientDTO.ClientResponseDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient(configKey = "client-api")
@Path("api/v1/client")
public interface ClientsApiClient {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RestResponse<ClientResponseDTO> createClient(@HeaderParam("Authorization") String authorization,
                                                 ClientRequestDTO requestClient);

}
