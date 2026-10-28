package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.OrderAccessDeniedException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrderAccessDeniedMapper implements ExceptionMapper<OrderAccessDeniedException> {

    @Override
    public Response toResponse(OrderAccessDeniedException exception) {

        return ErrorResponseBuilder.build("FORBIDDEN",
                                          exception.getMessage(),
                                          403);
    }
}
