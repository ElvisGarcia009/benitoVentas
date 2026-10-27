package com.benito.sales.resource;

import com.benito.sales.dto.LoginRequestDTO;
import com.benito.sales.dto.RegisterRequestDTO;
import com.benito.sales.dto.TokenResponseDTO;
import com.benito.sales.service.AuthService;
import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;
import io.quarkus.security.PermissionsAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;


@Path("api/v1/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class AuthResource {

    //DEPENDENCY INJECTION -> Service
    private final AuthService _authService;

    public AuthResource(AuthService authService)
    {
        this._authService = authService;
    }

    @POST
    @Path("/register")
    //NOTE: UNAUTHENTICATED USERS CAN CREATE AN ACCOUNT
    @RateLimited(bucket = "authentication", identityResolver = IpResolver.class)
    public RestResponse<Void> register(@Valid RegisterRequestDTO registerRequest)
    {
        _authService.register(registerRequest);
        return RestResponse.noContent();
    }

    @POST
    @Path("/login")
    @RateLimited(bucket = "authentication", identityResolver = IpResolver.class)
    public TokenResponseDTO login(@Valid LoginRequestDTO loginRequest)
    {
        //Quarkus return this with a 200 status code
        return _authService.login(loginRequest);
    }
}