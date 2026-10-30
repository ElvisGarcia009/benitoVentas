package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.helper.ErrorResponseBuilder;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnauthorizedMapper implements ExceptionMapper<UnauthorizedException> {

    @Override
    public Response toResponse(UnauthorizedException exception) {

        return ErrorResponseBuilder.build("UNAUTHORIZED",
                                          "Authentication is required to access this resource",
                                          401);

    }
}
