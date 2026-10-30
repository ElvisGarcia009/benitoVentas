package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.helper.ErrorResponseBuilder;
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

        return ErrorResponseBuilder.build("BAD REQUEST",
                                          errorMessage,
                                         400);
    }
}
