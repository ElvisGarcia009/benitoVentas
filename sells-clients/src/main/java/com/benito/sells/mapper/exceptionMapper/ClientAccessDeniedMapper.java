package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.ClientAccessDeniedException;
import com.benito.sells.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ClientAccessDeniedMapper implements ExceptionMapper<ClientAccessDeniedException> {

    @Override
    public Response toResponse(ClientAccessDeniedException exception) {

        return ErrorResponseBuilder.build("FORBIDDEN",
                                           exception.getMessage(),
                                          403);

    }
}
