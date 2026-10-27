package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.InvalidCredentialsException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidCredentialsMapper implements ExceptionMapper<InvalidCredentialsException> {

    @Override
    public Response toResponse(InvalidCredentialsException exception) {

        return ErrorResponseBuilder.build("UNAUTHORIZED",
                                            exception.getMessage(),
                                            401);
    }
}