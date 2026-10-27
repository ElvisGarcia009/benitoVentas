package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.SelfModificationException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SelfModificationMapper implements ExceptionMapper<SelfModificationException> {

    @Override
    public Response toResponse(SelfModificationException exception) {

        return ErrorResponseBuilder.build("FORBIDDEN",
                exception.getMessage(),
                403);
    }
}