package com.benito.sells.mapper;

import com.benito.sells.dto.ErrorDTO;
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

        ErrorDTO error = new ErrorDTO("INTERNAL SERVER ERROR",
                "An unexpected error has happened",
                500);

        return Response.status(error.errorStatusCode).entity(error).build();
    }
}
