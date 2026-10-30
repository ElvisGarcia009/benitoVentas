package com.benito.sales.resource;

import com.benito.sales.dto.LoginRequestDTO;
import com.benito.sales.dto.RegisterRequestDTO;
import com.benito.sales.dto.ServiceTokenRequestDTO;
import com.benito.sales.dto.TokenResponseDTO;
import com.benito.sales.service.AuthService;
import io.quarkiverse.bucket4j.runtime.RateLimited;
import io.quarkiverse.bucket4j.runtime.resolver.IpResolver;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import io.vertx.core.http.HttpServerRequest;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.X509Certificate;


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

    @POST
    @Path("/service-token")
    //ONLY FOR INTERNAL SERVICES: REQUIRES THE SERVICE CERTIFICATE (mTLS) AND ITS SECRET
    public TokenResponseDTO serviceToken(@Valid ServiceTokenRequestDTO request,
                                         @Context HttpServerRequest httpRequest)
    {
        return _authService.issueServiceToken(request, getCertificateName(httpRequest));
    }




    //HELPER: RETURNS THE NAME (CN) OF THE CLIENT CERTIFICATE, OR NULL IF NONE WAS PRESENTED
    private String getCertificateName(HttpServerRequest httpRequest)
    {
        try
        {
            X509Certificate certificate = (X509Certificate) httpRequest.sslSession().getPeerCertificates()[0];
            return certificate.getSubjectX500Principal().getName();
        }
        catch (SSLPeerUnverifiedException e)
        {
            return null;
        }
    }
}