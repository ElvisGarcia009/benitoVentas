package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.helper.ErrorResponseBuilder;
import io.quarkus.security.AuthenticationFailedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AuthenticationFailedMapper implements ExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {

        return ErrorResponseBuilder.build("AUTHENTICATION FAILED",
                                          "The token is invalid or has expired",
                                        401);
    }
}
