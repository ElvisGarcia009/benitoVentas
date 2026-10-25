package com.benito.sells.mapper;

import com.benito.sells.dto.ErrorDTO;
import com.benito.sells.exception.InvalidIdException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidIdMapper implements ExceptionMapper<InvalidIdException> {

    @Override
    public Response toResponse(InvalidIdException exception) {

        ErrorDTO error = new ErrorDTO("BAD REQUEST",
                                               exception.getMessage(),
                                               400);

        return Response.status(error.errorStatusCode).entity(error).build();
    }
}
