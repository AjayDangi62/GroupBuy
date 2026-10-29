package com.groupbuy.common.exception;

/**
 * Exception thrown when authentication fails due to incorrect credentials.
 * Resolves to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS");
    }

    public InvalidCredentialsException() {
        super("Invalid email or password.", "INVALID_CREDENTIALS");
    }
}
