package com.groupbuy.common.exception;

/**
 * Thrown when an invalid pricing request argument (such as a negative or zero quantity) is evaluated.
 */
public class InvalidPricingArgumentException extends BusinessException {

    public static final String ERROR_CODE = "INVALID_PRICING_ARGUMENT";

    public InvalidPricingArgumentException(String message) {
        super(message, ERROR_CODE);
    }
}
