package com.groupbuy.pricing.dto;

import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.pricing.domain.PricingTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Data Transfer Object for tier configuration inputs.
 */
@Schema(description = "Configurable pricing tier range and unit price")
public record PricingTierDto(
        @Schema(description = "Minimum eligible quantity for this tier (inclusive, must be >= 1)", example = "1")
        @NotNull(message = "Minimum quantity is required")
        @Positive(message = "Minimum quantity must be greater than zero")
        Integer minQuantity,

        @Schema(description = "Maximum eligible quantity for this tier (inclusive, null if open-ended)", example = "4")
        @Positive(message = "Maximum quantity must be greater than zero if provided")
        Integer maxQuantity,

        @Schema(description = "Unit price applied to eligible units in this tier", example = "2000.00")
        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
        BigDecimal unitPrice,

        @Schema(description = "ISO 4217 Currency code", example = "INR")
        String currency
) {
    public PricingTier toDomain() {
        return new PricingTier(
                minQuantity,
                maxQuantity,
                unitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE),
                (currency == null || currency.isBlank()) ? CurrencyConstants.DEFAULT_CURRENCY : currency.trim().toUpperCase()
        );
    }
}
