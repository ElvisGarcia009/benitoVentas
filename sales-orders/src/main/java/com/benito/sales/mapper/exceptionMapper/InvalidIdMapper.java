package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.InvalidIdException;
import com.benito.sales.helper.ErrorResponseBuilder;
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
