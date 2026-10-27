package com.benito.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorDTO {

    @JsonProperty("error_name")
    public String errorName;

    @JsonProperty("error_message")
    public String errorMessage;

    @JsonProperty("error_status_code")
    public int errorStatusCode;

    public ErrorDTO(String errorName, String errorMessage, int errorStatusCode)
    {
        this.errorStatusCode = errorStatusCode;
        this.errorName = errorName;
        this.errorMessage = errorMessage;
    }
}
