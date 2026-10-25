package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.exception.ProductNotFoundException;
import com.benito.sells.helper.ErrorResponseBuilder;
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
