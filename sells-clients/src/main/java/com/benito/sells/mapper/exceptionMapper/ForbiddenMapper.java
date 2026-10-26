package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.helper.ErrorResponseBuilder;
import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {

        return ErrorResponseBuilder.build("FORBIDDEN",
                                         exception.getMessage(),
                                        403);

    }
}
