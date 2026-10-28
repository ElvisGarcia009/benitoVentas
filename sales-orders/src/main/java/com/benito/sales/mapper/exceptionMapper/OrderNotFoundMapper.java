package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.OrderNotFoundException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OrderNotFoundMapper implements ExceptionMapper<OrderNotFoundException> {

    @Override
    public Response toResponse(OrderNotFoundException exception) {

        return ErrorResponseBuilder.build("NOT FOUND",
                                          exception.getMessage(),
                                          404);
    }
}
