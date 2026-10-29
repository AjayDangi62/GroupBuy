package com.groupbuy.participation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to join an active group-buy campaign")
public record JoinCampaignRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        @Schema(description = "Number of units to commit to buy", example = "2")
        Integer quantity
) {
}

