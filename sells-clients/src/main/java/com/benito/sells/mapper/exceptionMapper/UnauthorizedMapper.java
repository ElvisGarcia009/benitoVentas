package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.helper.ErrorResponseBuilder;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnauthorizedMapper implements ExceptionMapper<UnauthorizedException> {

    @Override
    public Response toResponse(UnauthorizedException exception) {

        return ErrorResponseBuilder.build("UNAUTHORIZED",
                                            exception.getMessage(),
                                            401);

    }
}
