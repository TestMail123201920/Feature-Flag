package com.company.featureflag.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base for all domain/application exceptions that should be translated into
 * a structured {@link ErrorResponse} by {@link GlobalExceptionHandler}
 * instead of leaking a stack trace or a generic 500.
 */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    protected ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
