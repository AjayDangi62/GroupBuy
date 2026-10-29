package com.groupbuy.common.exception;

/**
 * Details of an individual field validation error.
 */
public record FieldErrorItem(
        String field,
        Object rejectedValue,
        String message
) {
}
