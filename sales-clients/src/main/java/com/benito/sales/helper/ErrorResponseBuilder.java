package com.benito.sales.helper;

import com.benito.sales.dto.ErrorDTO;
import jakarta.ws.rs.core.Response;

public final class ErrorResponseBuilder {

    private ErrorResponseBuilder()
    {
        //YOU CANNOT MAKE AN INSTANCE OF THIS CLASS
    }

    public static Response build(String errorName, String errorMessage, int errorStatusCode)
    {
        ErrorDTO error = new ErrorDTO(errorName, errorMessage, errorStatusCode);

        return Response.status(error.errorStatusCode).entity(error).build();
    }
}
