package com.benito.sales.mapper.exceptionMapper;

import com.benito.sales.helper.ErrorResponseBuilder;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception>
{
    private final static Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof WebApplicationException webException)
            return webException.getResponse();

        //LOG THE ERROR TO THE SERVER CONSOLE
        LOG.error("Error not expected: ", exception);

        return ErrorResponseBuilder.build("INTERNAL SERVER ERROR",
                                        "An unexpected error has happened",
                                        500);
    }
}
