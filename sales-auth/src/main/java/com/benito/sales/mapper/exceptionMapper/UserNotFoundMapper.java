package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.exception.UserNotFoundException;
import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UserNotFoundMapper implements ExceptionMapper<UserNotFoundException> {

    @Override
    public Response toResponse(UserNotFoundException exception) {

        return ErrorResponseBuilder.build("NOT FOUND",
                exception.getMessage(),
                404);
    }
}