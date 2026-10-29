package com.groupbuy.common.constants;

import java.math.RoundingMode;

/**
 * Standard monetary constants and rounding policies for GroupBuy.
 */
public final class CurrencyConstants {

    private CurrencyConstants() {
        // Prevent instantiation of utility constant class
    }

    public static final String DEFAULT_CURRENCY = "INR";
    public static final int MONETARY_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;
}
