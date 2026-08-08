package com.company.featureflag.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String traceId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(code, message, Instant.now(), traceId, List.of());
    }

    public static ErrorResponse ofValidation(String code, String message, String traceId, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, Instant.now(), traceId, fieldErrors);
    }
}
