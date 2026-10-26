package com.benito.sells.resource;

import com.benito.sells.dto.ClientRequestDTO;
import com.benito.sells.dto.ClientResponseDTO;
import com.benito.sells.dto.ClientUpdateRequestDTO;
import com.benito.sells.dto.ClientsPaginatedDTO;
import com.benito.sells.exception.ClientAccessDeniedException;
import com.benito.sells.exception.InvalidIdException;
import com.benito.sells.service.ClientService;
import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;

@Path("api/v1/client")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {

    //DEPENDENCY INJECTION -> Service & jwt
    private final ClientService _clientService;
    private final JsonWebToken jwt;
    public ClientResource(ClientService clientService, JsonWebToken jwt)
    {
        this._clientService = clientService;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed("admin")
    public ClientsPaginatedDTO getAllClients(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                             @QueryParam("pageSize") @Positive @DefaultValue("10") @Max(30) int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _clientService.getAllClients(page, pageSize);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"admin", "client"})
    public ClientResponseDTO getClientById(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);

        if (!jwt.getGroups().contains("admin"))
            clientAccessVerification(id);

        //Quarkus return this with a 200 status code
        return _clientService.getClientById(validatedId);
    }


    @POST
    @PermitAll //NOTE: UNAUTHENTICATED USERS CAN CREATE AN ACCOUNT
    @RateLimited(bucket = "registration", identityResolver = IpResolver.class)
    public RestResponse<ClientResponseDTO> createClient(@Valid ClientRequestDTO requestClient,
                                                        @Context UriInfo uriInfo)
    {
        ClientResponseDTO createdClient = _clientService.createClient(requestClient, jwt.getSubject());

        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(createdClient.id)
                              .build();

        return RestResponse.ResponseBuilder.<ClientResponseDTO>created(location)
                                           .entity(createdClient)
                                           .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"admin", "client"})
    public ClientResponseDTO updateClient(@PathParam("id") String id, @Valid ClientUpdateRequestDTO requestClient) {

        ObjectId validatedId = validateId(id);

        if (!jwt.getGroups().contains("admin"))
            clientAccessVerification(id);

        //Quarkus return this with a 200 status code
        return _clientService.updateClient(validatedId, requestClient, jwt.getSubject());
    }


    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public RestResponse<Void> deleteClient(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);
        _clientService.deleteClient(validatedId, jwt.getSubject());
        return RestResponse.noContent();
    }




    //HELPERS
    private ObjectId validateId(String id)
    {
        if (!ObjectId.isValid(id))
            throw new InvalidIdException(id);

        return new ObjectId(id);
    }

    private void clientAccessVerification(String id)
    {
        if(!id.equals(jwt.getClaim("userId")))
            throw new ClientAccessDeniedException();
    }
}
