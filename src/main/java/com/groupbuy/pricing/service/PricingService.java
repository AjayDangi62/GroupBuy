package com.groupbuy.pricing.service;

import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.InvalidPricingArgumentException;
import com.groupbuy.common.exception.InvalidPricingPolicyException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.pricing.dto.PricingQuoteRequest;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.dto.PricingTierDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Service orchestrating pricing calculations based on volume tiers.
 * Tier selection is strictly data-driven with no hardcoded if/else evaluation chains.
 */
@Service
public class PricingService {

    private final PricingPolicyValidator policyValidator;

    /**
     * Documented server-managed sample pricing policy (Wireless Headphones model):
     * Tier 1: 1–4 units: ₹2,000/unit
     * Tier 2: 5–9 units: ₹1,800/unit
     * Tier 3: 10–19 units: ₹1,600/unit
     * Tier 4: 20+ units: ₹1,400/unit
     */
    private final List<PricingTier> defaultSamplePolicy;

    public PricingService(PricingPolicyValidator policyValidator) {
        this.policyValidator = policyValidator;
        this.defaultSamplePolicy = List.of(
                new PricingTier(1, 4, new BigDecimal("2000.00"), CurrencyConstants.DEFAULT_CURRENCY),
                new PricingTier(5, 9, new BigDecimal("1800.00"), CurrencyConstants.DEFAULT_CURRENCY),
                new PricingTier(10, 19, new BigDecimal("1600.00"), CurrencyConstants.DEFAULT_CURRENCY),
                new PricingTier(20, null, new BigDecimal("1400.00"), CurrencyConstants.DEFAULT_CURRENCY)
        );
        // Self-validate the server default policy at startup to ensure integrity
        this.policyValidator.validatePolicy(this.defaultSamplePolicy);
    }

    /**
     * Calculates a pricing quote for the given request.
     * Uses the provided custom tiers if present; otherwise applies the server sample policy.
     */
    public PricingQuoteResponse calculateQuote(PricingQuoteRequest request) {
        if (request.eligibleQuantity() == null || request.eligibleQuantity() <= 0) {
            throw new InvalidPricingArgumentException(
                    "Eligible quantity must be a positive integer (> 0), received: " + request.eligibleQuantity()
            );
        }

        int quantity = request.eligibleQuantity();
        List<PricingTier> activePolicy;
        String policyType;

        if (request.customTiers() != null && !request.customTiers().isEmpty()) {
            activePolicy = request.customTiers().stream()
                    .map(PricingTierDto::toDomain)
                    .toList();
            policyValidator.validatePolicy(activePolicy);
            policyType = "CUSTOM_CONFIGURED";
        } else {
            activePolicy = this.defaultSamplePolicy;
            policyType = "SERVER_DEFAULT_SAMPLE";
        }

        return evaluateAgainstPolicy(quantity, activePolicy, policyType);
    }

    /**
     * Evaluates quantity directly against any validated list of pricing tiers.
     */
    public PricingQuoteResponse calculateQuote(int quantity, List<PricingTier> policy, String policyType) {
        if (quantity <= 0) {
            throw new InvalidPricingArgumentException(
                    "Eligible quantity must be a positive integer (> 0), received: " + quantity
            );
        }

        policyValidator.validatePolicy(policy);
        return evaluateAgainstPolicy(quantity, policy, policyType);
    }

    private PricingQuoteResponse evaluateAgainstPolicy(int quantity, List<PricingTier> policy, String policyType) {
        // Data-driven tier lookup: locate the unique tier where min <= quantity <= max (or max is null)
        for (int i = 0; i < policy.size(); i++) {
            PricingTier tier = policy.get(i);
            if (tier.matches(quantity)) {
                BigDecimal unitPrice = tier.unitPrice().setScale(
                        CurrencyConstants.MONETARY_SCALE,
                        CurrencyConstants.DEFAULT_ROUNDING_MODE
                );

                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(
                        CurrencyConstants.MONETARY_SCALE,
                        CurrencyConstants.DEFAULT_ROUNDING_MODE
                );

                String summary = String.format(
                        "Tier %d: [%d..%s units] @ %s %s each",
                        i + 1,
                        tier.minQuantity(),
                        tier.isOpenEnded() ? "+" : String.valueOf(tier.maxQuantity()),
                        tier.currency(),
                        unitPrice.toPlainString()
                );

                return new PricingQuoteResponse(
                        quantity,
                        unitPrice,
                        subtotal,
                        tier.currency(),
                        tier.minQuantity(),
                        tier.maxQuantity(),
                        i,
                        policyType,
                        summary
                );
            }
        }

        // Defensive: with valid policies, this line is unreachable because the last tier is open-ended
        throw new InvalidPricingPolicyException(
                "No matching tier found for quantity " + quantity + " in the provided policy."
        );
    }

    public List<PricingTier> getDefaultSamplePolicy() {
        return Collections.unmodifiableList(defaultSamplePolicy);
    }
}
