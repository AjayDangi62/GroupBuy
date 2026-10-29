package com.groupbuy.campaign.dto;

import com.groupbuy.pricing.dto.PricingTierDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

@Schema(description = "Group-buy campaign creation request payload")
public record CampaignCreateRequest(
        @NotNull(message = "Product ID is required")
        @Schema(description = "ID of catalog product being featured", example = "1")
        Long productId,

        @NotBlank(message = "Campaign title is required")
        @Schema(description = "Public title of the campaign", example = "Sony WH-1000XM5 Early Bird Group Buy")
        String title,

        @Schema(description = "Campaign terms and description", example = "Unlock up to 40% off when 20+ units are ordered!")
        String description,

        @NotNull(message = "Target minimum quantity is required")
        @Positive(message = "Target minimum quantity must be greater than zero")
        @Schema(description = "Minimum total units needed across all buyers for campaign success", example = "10")
        Integer targetMinQuantity,

        @Positive(message = "Maximum campaign quantity must be greater than zero if provided")
        @Schema(description = "Cap on total units allowed in campaign (optional)", example = "100")
        Integer maxCampaignQuantity,

        @Positive(message = "Minimum participants must be greater than zero")
        @Schema(description = "Minimum distinct buyers required for campaign success", example = "3")
        Integer minParticipants,

        @NotNull(message = "Start time is required")
        @Schema(description = "Campaign launch time in UTC", example = "2026-03-01T00:00:00Z")
        Instant startsAt,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        @Schema(description = "Campaign closing deadline in UTC", example = "2026-03-07T23:59:59Z")
        Instant endsAt,

        @Schema(description = "Currency code", example = "INR")
        String currency,

        @NotEmpty(message = "At least one pricing tier must be configured")
        @Valid
        @Schema(description = "Tiered volume discount rules")
        List<PricingTierDto> pricingTiers
) {
}

