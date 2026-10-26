package com.benito.sells.mapper.exceptionMapper;

import com.benito.sells.helper.ErrorResponseBuilder;
import io.quarkiverse.bucket4j.runtime.RateLimitException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TooManyRequestsMapper implements ExceptionMapper<RateLimitException> {

    @Override
    public Response toResponse(RateLimitException exception) {

        long secondsToWait = exception.getWaitTimeInMilliSeconds() / 1000;

        return ErrorResponseBuilder.build("TOO MANY REQUESTS",
                                           "Retry after: " + secondsToWait + "s",
                                          429);
    }
}
