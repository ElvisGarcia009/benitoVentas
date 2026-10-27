package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.ConflictException;
import com.benito.sells.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConflictMapper implements ExceptionMapper<ConflictException> {

    @Override
    public Response toResponse(ConflictException exception) {
        return ErrorResponseBuilder.build("CONFLICT",
                                           exception.getMessage(),
                                          409);
    }
}
