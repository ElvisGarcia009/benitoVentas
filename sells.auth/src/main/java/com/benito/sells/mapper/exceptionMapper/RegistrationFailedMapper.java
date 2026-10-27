package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.RegistrationFailedException;
import com.benito.sells.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RegistrationFailedMapper implements ExceptionMapper<RegistrationFailedException> {

    @Override
    public Response toResponse(RegistrationFailedException exception) {

        return ErrorResponseBuilder.build("SERVICE UNAVAILABLE",
                exception.getMessage(),
                503);
    }
}