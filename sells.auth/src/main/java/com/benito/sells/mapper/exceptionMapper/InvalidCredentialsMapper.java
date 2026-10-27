package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.InvalidCredentialsException;
import com.benito.sells.helper.ErrorResponseBuilder;
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