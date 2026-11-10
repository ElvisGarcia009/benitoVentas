package com.benito.sales.exception;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String serviceName)
    {
        super("The " + serviceName + " service could not process the request");
    }
}
