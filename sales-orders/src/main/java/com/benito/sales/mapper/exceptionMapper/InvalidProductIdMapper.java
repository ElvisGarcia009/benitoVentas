package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.InvalidProductIdException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidProductIdMapper implements ExceptionMapper<InvalidProductIdException> {

    @Override
    public Response toResponse(InvalidProductIdException exception) {

        return ErrorResponseBuilder.build("BAD REQUEST",
                                          exception.getMessage(),
                                          400);
    }
}
