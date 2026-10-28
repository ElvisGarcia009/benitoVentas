package com.benito.sales.exception;

public class ServiceNotAvailableException extends RuntimeException {
    public ServiceNotAvailableException(String serviceName)
    {
        super("The " + serviceName + " service is not available, please try again later");
    }
}
