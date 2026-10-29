package com.groupbuy.common.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BusinessException {

    public static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE);
    }
}
