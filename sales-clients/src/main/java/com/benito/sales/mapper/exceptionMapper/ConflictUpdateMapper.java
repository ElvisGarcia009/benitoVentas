package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.ConflictUpdateException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConflictUpdateMapper implements ExceptionMapper<ConflictUpdateException> {

    @Override
    public Response toResponse(ConflictUpdateException exception) {

        return ErrorResponseBuilder.build("CONFLICT",
                                          exception.getMessage(),
                                          409);

    }
}
