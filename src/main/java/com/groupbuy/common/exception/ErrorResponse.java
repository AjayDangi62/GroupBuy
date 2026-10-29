package com.groupbuy.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard REST error response payload for consistent client error handling.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, null);
    }

    public static ErrorResponse of(int status, String errorCode, String message, String path, List<FieldErrorItem> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, fieldErrors);
    }
}
