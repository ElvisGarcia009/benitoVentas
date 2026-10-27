package com.benito.sells.restClient;

import com.benito.sells.dto.ClientRequestDTO;
import com.benito.sells.dto.ClientResponseDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestResponse;

@RegisterRestClient(configKey = "clients-api")
@Path("api/v1/client")
public interface ClientsApiClient {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    RestResponse<ClientResponseDTO> createClient(@HeaderParam("Authorization") String authorization,
                                                 ClientRequestDTO requestClient);

}
