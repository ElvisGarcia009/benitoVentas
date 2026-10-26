package com.benito.sells.resource;

import com.benito.sells.dto.LoginRequestDTO;
import com.benito.sells.dto.RegisterRequestDTO;
import com.benito.sells.dto.TokenResponseDTO;
import com.benito.sells.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;

import java.awt.*;


@Path("api/v1/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService _authService;

    public AuthResource(AuthService authService)
    {
        this._authService = authService;
    }

    @POST
    @Path("/register")
    public RestResponse<Void> register(@Valid RegisterRequestDTO registerRequest,
                                       @Context UriInfo uriInfo)
    {
         _authService.register(registerRequest);
        return RestResponse.noContent();
    }

//    @POST
//    @Path("/login")
//    @PermitAll
//    public RestResponse<TokenResponseDTO> login(@Valid LoginRequestDTO loginRequest)
//    {
//        //TokenResponseDTO accessToken = _authService.login(loginRequest);
//
//        return RestResponse.ok(accessToken);
//    }


}
