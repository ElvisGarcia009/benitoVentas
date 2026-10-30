package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.InvalidServiceCredentialsException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidServiceCredentialsMapper implements ExceptionMapper<InvalidServiceCredentialsException> {

    @Override
    public Response toResponse(InvalidServiceCredentialsException exception) {
        return ErrorResponseBuilder.build("UNAUTHORIZED",
                                          exception.getMessage(),
                                          401);
    }
}
