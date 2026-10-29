package com.groupbuy.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Request payload for pricing quote evaluation.
 * If customTiers is omitted or empty, the server evaluates using the documented
 * server-managed sample policy (e.g., Wireless Headphones tiers).
 */
@Schema(description = "Pricing quote request parameters")
public record PricingQuoteRequest(
        @Schema(description = "The eligible number of participant units to calculate pricing for", example = "7")
        @NotNull(message = "Eligible quantity is required")
        @Positive(message = "Eligible quantity must be greater than zero")
        Integer eligibleQuantity,

        @Schema(description = "Optional custom pricing policy tiers. When null or empty, the server-managed sample policy is used.")
        @Valid
        List<PricingTierDto> customTiers
) {
}
