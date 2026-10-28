package com.benito.sales.resource;

import com.benito.sales.dto.*;
import com.benito.sales.exception.ClientAccessDeniedException;
import com.benito.sales.exception.InvalidIdException;
import com.benito.sales.service.ClientService;
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
    @RolesAllowed({"admin", "client"}) //A client can ask for his own information
    public ClientResponseDTO getClientById(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);
        boolean isAdmin = jwt.getGroups().contains("admin");

        //Quarkus return this with a 200 status code
        return _clientService.getClientById(validatedId, jwt.getClaim("userId"), isAdmin);
    }


    @POST
    @RolesAllowed("aService") //NOTE: NEW CLIENTS CAN ONLY BE REGISTERED BY THE AUTH SERVICE
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
    @RolesAllowed({"admin", "client"}) //NOTE: CLIENT CAN UPDATE HIS INFORMATION
    public ClientResponseDTO updateClient(@PathParam("id") String id, @Valid ClientUpdateRequestDTO requestClient) {

        ObjectId validatedId = validateId(id);
        boolean isAdmin = jwt.getGroups().contains("admin");

        //Quarkus return this with a 200 status code
        return _clientService.updateClient(validatedId, requestClient,
                jwt.getClaim("userId"), isAdmin, jwt.getSubject());
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


    @POST
    @Path("/{id}/balance/debit")
    @RolesAllowed({"admin", "client"})
    public ClientResponseDTO debitBalance(@PathParam("id") String id, @Valid BalanceRequestDTO requestBalance)
    {
        ObjectId validatedId = validateId(id);
        boolean isAdmin = jwt.getGroups().contains("admin");

        //Quarkus return this with a 200 status code
        return _clientService.debitBalance(validatedId, requestBalance.amount,
                jwt.getClaim("userId"), isAdmin, jwt.getSubject());
    }

    @POST
    @Path("/{id}/balance/credit")
    @RolesAllowed("admin")
    public ClientResponseDTO creditBalance(@PathParam("id") String id, @Valid BalanceRequestDTO requestBalance)
    {
        ObjectId validatedId = validateId(id);

        //Quarkus return this with a 200 status code
        return _clientService.creditBalance(validatedId, requestBalance.amount, jwt.getSubject());
    }




    //HELPERS
    private ObjectId validateId(String id)
    {
        if (!ObjectId.isValid(id))
            throw new InvalidIdException(id);

        return new ObjectId(id);
    }
}
