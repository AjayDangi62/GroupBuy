package com.groupbuy.pricing.domain;

import java.math.BigDecimal;

/**
 * Immutable domain representation of a volume-based pricing tier.
 * Inclusive boundary convention: [minQuantity, maxQuantity].
 * When maxQuantity is null, the tier represents an open-ended upper bound [minQuantity, +infinity).
 */
public record PricingTier(
        int minQuantity,
        Integer maxQuantity,
        BigDecimal unitPrice,
        String currency
) {
    public PricingTier {
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }

    /**
     * Checks if the given eligible quantity satisfies this tier's inclusive range.
     */
    public boolean matches(int quantity) {
        if (quantity < minQuantity) {
            return false;
        }
        return maxQuantity == null || quantity <= maxQuantity;
    }

    /**
     * Indicates whether this tier has no upper bound.
     */
    public boolean isOpenEnded() {
        return maxQuantity == null;
    }
}
