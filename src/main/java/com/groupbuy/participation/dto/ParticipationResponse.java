package com.groupbuy.participation.dto;

import com.groupbuy.participation.entity.Participation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Group-buy participation details")
public record ParticipationResponse(
        @Schema(description = "Participation ID", example = "10")
        Long id,

        @Schema(description = "Campaign ID", example = "1")
        Long campaignId,

        @Schema(description = "Campaign title", example = "Sony WH-1000XM5 Early Bird Group Buy")
        String campaignTitle,

        @Schema(description = "Product ID", example = "1")
        Long productId,

        @Schema(description = "Product name", example = "Sony WH-1000XM5")
        String productName,

        @Schema(description = "Buyer user ID", example = "3")
        Long userId,

        @Schema(description = "Buyer username", example = "alice_buyer")
        String username,

        @Schema(description = "Committed unit quantity", example = "2")
        int quantity,

        @Schema(description = "Authorized unit price at time of joining", example = "1800.00")
        BigDecimal authorizedUnitPrice,

        @Schema(description = "Authorized total amount held", example = "3600.00")
        BigDecimal authorizedTotalAmount,

        @Schema(description = "Final settled unit price (null until campaign succeeds)", example = "1600.00")
        BigDecimal finalUnitPrice,

        @Schema(description = "Final settled total amount (null until campaign succeeds)", example = "3200.00")
        BigDecimal finalTotalAmount,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "Participation status", example = "CONFIRMED")
        String status,

        @Schema(description = "Join timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static ParticipationResponse fromEntity(Participation p) {
        Long campaignId = p.getCampaign() != null ? p.getCampaign().getId() : null;
        String campaignTitle = p.getCampaign() != null ? p.getCampaign().getTitle() : null;
        Long productId = (p.getCampaign() != null && p.getCampaign().getProduct() != null) ? p.getCampaign().getProduct().getId() : null;
        String productName = (p.getCampaign() != null && p.getCampaign().getProduct() != null) ? p.getCampaign().getProduct().getName() : null;
        String currency = p.getCampaign() != null ? p.getCampaign().getCurrency() : "INR";
        Long userId = p.getUser() != null ? p.getUser().getId() : null;
        String username = p.getUser() != null ? p.getUser().getUsername() : null;

        return new ParticipationResponse(
                p.getId(),
                campaignId,
                campaignTitle,
                productId,
                productName,
                userId,
                username,
                p.getQuantity(),
                p.getAuthorizedUnitPrice(),
                p.getAuthorizedTotalAmount(),
                p.getFinalUnitPrice(),
                p.getFinalTotalAmount(),
                currency,
                p.getStatus().name(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}

