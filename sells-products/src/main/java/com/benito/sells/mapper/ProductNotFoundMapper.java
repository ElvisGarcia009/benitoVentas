package com.benito.sells.mapper;

import com.benito.sells.dto.ErrorDTO;
import com.benito.sells.exception.ProductNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProductNotFoundMapper implements ExceptionMapper<ProductNotFoundException> {

    @Override
    public Response toResponse(ProductNotFoundException exception) {

        ErrorDTO error = new ErrorDTO("NOT FOUND",
                                               exception.getMessage(),
                                               404);

        return Response.status(error.errorStatusCode).entity(error).build();
    }

}
