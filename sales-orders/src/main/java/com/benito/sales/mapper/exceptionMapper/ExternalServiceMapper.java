package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.ExternalServiceException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ExternalServiceMapper implements ExceptionMapper<ExternalServiceException> {

    @Override
    public Response toResponse(ExternalServiceException exception) {

        return ErrorResponseBuilder.build("BAD GATEWAY",
                                          exception.getMessage(),
                                          502);
    }
}
