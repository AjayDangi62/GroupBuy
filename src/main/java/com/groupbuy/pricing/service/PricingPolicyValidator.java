package com.groupbuy.pricing.service;

import com.groupbuy.common.exception.InvalidPricingPolicyException;
import com.groupbuy.pricing.domain.PricingTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validates pricing tier configurations against strict domain invariants.
 *
 * Invariant rules enforced:
 * 1. Policy must not be null or empty.
 * 2. The first tier must begin at quantity 1 (full baseline coverage).
 * 3. Every tier must have minQuantity >= 1.
 * 4. Every tier must have a non-negative unit price (unitPrice >= 0).
 * 5. If maxQuantity is specified, it must be >= minQuantity (no reversed boundaries).
 * 6. Minimum quantities must be strictly ascending.
 * 7. Tiers must be strictly contiguous: tier[i+1].minQuantity == tier[i].maxQuantity + 1 (no gaps, no overlaps).
 * 8. Only the final tier may be open-ended (maxQuantity == null).
 * 9. The final tier MUST be open-ended (guarantees coverage for all eligible quantities without ceiling cliff).
 * 10. All tiers must use a consistent currency.
 */
@Component
public class PricingPolicyValidator {

    public void validatePolicy(List<PricingTier> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new InvalidPricingPolicyException("Pricing policy must contain at least one configured tier.");
        }

        String baseCurrency = tiers.get(0).currency();

        // Rule 2: First tier must start at 1
        if (tiers.get(0).minQuantity() != 1) {
            throw new InvalidPricingPolicyException(
                    "First pricing tier must start at quantity 1, but found: " + tiers.get(0).minQuantity()
            );
        }

        int tierCount = tiers.size();

        for (int i = 0; i < tierCount; i++) {
            PricingTier tier = tiers.get(i);
            int tierIndex = i + 1;

            // Rule 3: Positive minimum quantity
            if (tier.minQuantity() <= 0) {
                throw new InvalidPricingPolicyException(
                        "Tier " + tierIndex + " minQuantity must be positive (> 0), but found: " + tier.minQuantity()
                );
            }

            // Rule 4: Non-negative price
            if (tier.unitPrice() == null || tier.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidPricingPolicyException(
                        "Tier " + tierIndex + " unitPrice must be non-negative (>= 0), but found: " + tier.unitPrice()
                );
            }

            // Rule 10: Consistent currency
            if (!tier.currency().equalsIgnoreCase(baseCurrency)) {
                throw new InvalidPricingPolicyException(
                        "Tier " + tierIndex + " currency (" + tier.currency() +
                        ") does not match policy currency (" + baseCurrency + ")."
                );
            }

            // Rule 5: Non-reversed boundaries
            if (tier.maxQuantity() != null && tier.maxQuantity() < tier.minQuantity()) {
                throw new InvalidPricingPolicyException(
                        "Tier " + tierIndex + " has reversed boundaries: minQuantity (" +
                        tier.minQuantity() + ") is greater than maxQuantity (" + tier.maxQuantity() + ")."
                );
            }

            boolean isLastTier = (i == tierCount - 1);

            if (!isLastTier) {
                // Rule 8: Non-terminal tiers cannot be open-ended
                if (tier.isOpenEnded()) {
                    throw new InvalidPricingPolicyException(
                            "Tier " + tierIndex + " is open-ended (no maxQuantity), but open-ended tiers are only permitted as the final tier."
                    );
                }

                PricingTier nextTier = tiers.get(i + 1);

                // Rule 6: Strictly ordered minimums
                if (nextTier.minQuantity() <= tier.minQuantity()) {
                    throw new InvalidPricingPolicyException(
                            "Tiers must be strictly ordered by minQuantity. Tier " + (tierIndex + 1) +
                            " minQuantity (" + nextTier.minQuantity() + ") must be greater than Tier " +
                            tierIndex + " minQuantity (" + tier.minQuantity() + ")."
                    );
                }

                // Rule 7: Overlaps and Gaps
                int expectedNextMin = tier.maxQuantity() + 1;
                if (nextTier.minQuantity() < expectedNextMin) {
                    throw new InvalidPricingPolicyException(
                            "Overlapping tiers detected: Tier " + tierIndex + " ends at " +
                            tier.maxQuantity() + ", but Tier " + (tierIndex + 1) +
                            " starts at " + nextTier.minQuantity() + "."
                    );
                } else if (nextTier.minQuantity() > expectedNextMin) {
                    throw new InvalidPricingPolicyException(
                            "Gap between tiers detected: Tier " + tierIndex + " ends at " +
                            tier.maxQuantity() + ", but Tier " + (tierIndex + 1) +
                            " starts at " + nextTier.minQuantity() + ". Missing coverage for [" +
                            expectedNextMin + ".." + (nextTier.minQuantity() - 1) + "]."
                    );
                }
            } else {
                // Rule 9: The final tier must be open-ended
                if (!tier.isOpenEnded()) {
                    throw new InvalidPricingPolicyException(
                            "The final tier (Tier " + tierIndex + ") must be open-ended (maxQuantity = null) " +
                            "to ensure all quantities from " + tier.minQuantity() + " and above are covered."
                    );
                }
            }
        }
    }
}
