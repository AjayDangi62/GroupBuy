package com.groupbuy.common.exception;

/**
 * Exception thrown when attempting to create a resource that already exists (e.g. duplicate email/username).
 * Resolves to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String message, String errorCode) {
        super(message, errorCode);
    }
}
