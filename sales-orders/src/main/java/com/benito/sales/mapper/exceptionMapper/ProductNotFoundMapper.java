package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.ProductNotFoundException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProductNotFoundMapper implements ExceptionMapper<ProductNotFoundException> {

    @Override
    public Response toResponse(ProductNotFoundException exception) {

        return ErrorResponseBuilder.build("NOT FOUND",
                                          exception.getMessage(),
                                          404);
    }
}
