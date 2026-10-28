package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.ClientNotFoundException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ClientNotFoundMapper implements ExceptionMapper<ClientNotFoundException> {

    @Override
    public Response toResponse(ClientNotFoundException exception) {

        return ErrorResponseBuilder.build("NOT FOUND",
                                          exception.getMessage(),
                                          404);
    }
}
