package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.ServiceNotAvailableException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ServiceNotAvailableMapper implements ExceptionMapper<ServiceNotAvailableException> {

    @Override
    public Response toResponse(ServiceNotAvailableException exception) {

        return ErrorResponseBuilder.build("SERVICE UNAVAILABLE",
                                          exception.getMessage(),
                                          503);
    }
}
