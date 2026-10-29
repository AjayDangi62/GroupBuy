package com.groupbuy.campaign.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Campaign update request payload")
public record CampaignUpdateRequest(
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        @Schema(description = "Updated title")
        String title,

        @Schema(description = "Updated description")
        String description,

        @Positive(message = "Target minimum quantity must be greater than zero")
        @Schema(description = "Updated target minimum quantity")
        Integer targetMinQuantity,

        @Positive(message = "Max campaign quantity must be greater than zero")
        @Schema(description = "Updated max campaign quantity")
        Integer maxCampaignQuantity,

        @Positive(message = "Min participants must be greater than zero")
        @Schema(description = "Updated minimum participants")
        Integer minParticipants,

        @Future(message = "End time must be in the future")
        @Schema(description = "Updated campaign closing deadline")
        Instant endsAt
) {
}

