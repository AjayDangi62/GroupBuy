package com.groupbuy.pricing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Structured breakdown of the evaluated pricing quote.
 */
@Schema(description = "Detailed breakdown of the evaluated pricing quote")
public record PricingQuoteResponse(
        @Schema(description = "Eligible quantity evaluated", example = "7")
        int eligibleQuantity,

        @Schema(description = "Selected unit price for the qualifying tier", example = "1800.00")
        BigDecimal unitPrice,

        @Schema(description = "Total subtotal amount (eligibleQuantity * unitPrice)", example = "12600.00")
        BigDecimal subtotal,

        @Schema(description = "Monetary currency code", example = "INR")
        String currency,

        @Schema(description = "Lower bound of the matched tier (inclusive)", example = "5")
        int tierMinQuantity,

        @Schema(description = "Upper bound of the matched tier (inclusive, null if open-ended)", example = "9")
        Integer tierMaxQuantity,

        @Schema(description = "0-based index of the matched tier in policy", example = "1")
        int tierIndex,

        @Schema(description = "Policy source applied (SERVER_DEFAULT_SAMPLE or CUSTOM_CONFIGURED)", example = "SERVER_DEFAULT_SAMPLE")
        String policyType,

        @Schema(description = "Human-readable summary of tier match", example = "Tier 2: [5..9 units] @ INR 1800.00 each")
        String summary
) {
}
