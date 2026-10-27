package com.benito.sells.resource;

import com.benito.sells.dto.UserRequestDTO;
import com.benito.sells.dto.UserResponseDTO;
import com.benito.sells.dto.UserRolesRequestDTO;
import com.benito.sells.dto.UsersPaginatedDTO;
import com.benito.sells.exception.InvalidIdException;
import com.benito.sells.exception.SelfModificationException;
import com.benito.sells.service.UserService;
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

@Path("api/v1/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class UserResource {

    //DEPENDENCY INJECTION -> Service & jwt
    private final UserService _userService;
    private final JsonWebToken jwt;
    public UserResource(UserService userService, JsonWebToken jwt)
    {
        this._userService = userService;
        this.jwt = jwt;
    }

    @GET
    public UsersPaginatedDTO getAllUsers(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                         @QueryParam("pageSize") @Positive @DefaultValue("10") @Max(30) int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _userService.getAllUsers(page, pageSize);
    }

    @GET
    @Path("/{id}")
    public UserResponseDTO getUserById(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);

        //Quarkus return this with a 200 status code
        return _userService.getUserById(validatedId);
    }


    @POST
    public RestResponse<UserResponseDTO> createUser(@Valid UserRequestDTO requestUser,
                                                    @Context UriInfo uriInfo)
    {
        UserResponseDTO createdUser = _userService.createUser(requestUser, jwt.getSubject());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(createdUser.id)
                .build();

        return RestResponse.ResponseBuilder.<UserResponseDTO>created(location)
                .entity(createdUser)
                .build();
    }

    @PUT
    @Path("/{id}/roles")
    public UserResponseDTO updateUserRoles(@PathParam("id") String id, @Valid UserRolesRequestDTO requestRoles)
    {
        ObjectId validatedId = validateId(id);
        selfModificationVerification(id);

        //Quarkus return this with a 200 status code
        return _userService.updateUserRoles(validatedId, requestRoles, jwt.getSubject());
    }


    @DELETE
    @Path("/{id}")
    public RestResponse<Void> deleteUser(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);
        selfModificationVerification(id);

        _userService.deleteUser(validatedId, jwt.getSubject());
        return RestResponse.noContent();
    }




    //HELPERS
    private ObjectId validateId(String id)
    {
        if (!ObjectId.isValid(id))
            throw new InvalidIdException(id);

        return new ObjectId(id);
    }

    //NOTE: AN ADMIN CANNOT CHANGE HIS OWN ROLES OR DEACTIVATE HIMSELF, TO AVOID LEAVING THE SYSTEM WITHOUT ADMINS
    private void selfModificationVerification(String id)
    {
        if (id.equals(jwt.getClaim("userId")))
            throw new SelfModificationException();
    }
}