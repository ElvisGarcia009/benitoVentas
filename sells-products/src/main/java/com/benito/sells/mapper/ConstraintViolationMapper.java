package com.benito.sells.mapper;

import com.benito.sells.dto.ErrorDTO;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {

        //MAP THE VIOLATIONS IN A SINGLE MESSAGE
        String errorMessage = exception.getConstraintViolations().stream()
                    .map(constraintViolation -> constraintViolation.getPropertyPath() + ": " + constraintViolation.getMessage())
                    .collect(Collectors.joining(", "));

        ErrorDTO error = new ErrorDTO("BAD REQUEST",
                                       errorMessage,
                                      500);

        return Response.status(error.errorStatusCode).entity(error).build();
    }
}
