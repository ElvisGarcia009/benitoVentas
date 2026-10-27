package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.SelfModificationException;
import com.benito.sells.helper.ErrorResponseBuilder;
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