package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.InsufficientBalanceException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InsufficientBalanceMapper implements ExceptionMapper<InsufficientBalanceException> {

    @Override
    public Response toResponse(InsufficientBalanceException exception) {

        return ErrorResponseBuilder.build("UNPROCESSABLE ENTITY",
                                          exception.getMessage(),
                                          422);
    }
}
