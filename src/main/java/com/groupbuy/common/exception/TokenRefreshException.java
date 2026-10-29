package com.groupbuy.common.exception;

/**
 * Exception thrown when a refresh token operation fails (invalid, expired, or reuse detected).
 * Resolves to HTTP 401 Unauthorized.
 */
public class TokenRefreshException extends BusinessException {

    public TokenRefreshException(String message) {
        super(message, "TOKEN_REFRESH_FAILED");
    }

    public TokenRefreshException(String message, String errorCode) {
        super(message, errorCode);
    }
}
