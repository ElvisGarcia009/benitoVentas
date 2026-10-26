package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.InvalidIdException;
import com.benito.sells.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidIdMapper implements ExceptionMapper<InvalidIdException> {

    @Override
    public Response toResponse(InvalidIdException exception) {

        return ErrorResponseBuilder.build("BAD REQUEST",
                exception.getMessage(),
                400);
    }
}
