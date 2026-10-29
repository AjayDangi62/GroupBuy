package com.groupbuy.common.exception;

/**
 * Thrown when a pricing tier configuration violates domain validation invariants
 * (e.g. gaps, overlaps, reversed boundaries, invalid open-ended tiers).
 */
public class InvalidPricingPolicyException extends BusinessException {

    public static final String ERROR_CODE = "INVALID_PRICING_POLICY";

    public InvalidPricingPolicyException(String message) {
        super(message, ERROR_CODE);
    }

    public InvalidPricingPolicyException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}
