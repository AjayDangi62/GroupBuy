package com.groupbuy.campaign.dto;

import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.pricing.dto.PricingTierDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Group-buy campaign summary response")
public record CampaignResponse(
        @Schema(description = "Campaign ID", example = "1")
        Long id,

        @Schema(description = "Featured product ID", example = "1")
        Long productId,

        @Schema(description = "Featured product name", example = "Sony WH-1000XM5")
        String productName,

        @Schema(description = "Product SKU", example = "WH-1000XM5-BLK")
        String productSku,

        @Schema(description = "Product base price", example = "2499.00")
        BigDecimal productBasePrice,

        @Schema(description = "Product image URL")
        String productImageUrl,

        @Schema(description = "Seller ID", example = "2")
        Long sellerId,

        @Schema(description = "Seller name", example = "Acme Electronics")
        String sellerName,

        @Schema(description = "Campaign title", example = "Sony WH-1000XM5 Group Buy")
        String title,

        @Schema(description = "Campaign description")
        String description,

        @Schema(description = "Target minimum units for campaign success", example = "10")
        int targetMinQuantity,

        @Schema(description = "Maximum unit limit (optional)", example = "100")
        Integer maxCampaignQuantity,

        @Schema(description = "Current committed paid units", example = "7")
        int currentPaidUnits,

        @Schema(description = "Minimum distinct participants required", example = "3")
        int minParticipants,

        @Schema(description = "Current distinct participant count", example = "5")
        int currentParticipants,

        @Schema(description = "Campaign lifecycle status", example = "ACTIVE")
        String status,

        @Schema(description = "Campaign start time in UTC")
        Instant startsAt,

        @Schema(description = "Campaign end time in UTC")
        Instant endsAt,

        @Schema(description = "Settled final unit price across all participants (null while active)")
        BigDecimal finalUnitPrice,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "Configured volume pricing tiers")
        List<PricingTierDto> pricingTiers,

        @Schema(description = "Creation timestamp")
        Instant createdAt
) {
    public static CampaignResponse fromEntity(Campaign campaign) {
        Long productId = campaign.getProduct() != null ? campaign.getProduct().getId() : null;
        String productName = campaign.getProduct() != null ? campaign.getProduct().getName() : null;
        String productSku = campaign.getProduct() != null ? campaign.getProduct().getSku() : null;
        BigDecimal productBasePrice = campaign.getProduct() != null ? campaign.getProduct().getBasePrice() : null;
        String productImageUrl = campaign.getProduct() != null ? campaign.getProduct().getImageUrl() : null;

        Long sellerId = campaign.getSeller() != null ? campaign.getSeller().getId() : null;
        String sellerName = campaign.getSeller() != null ? campaign.getSeller().getFullName() : null;

        List<PricingTierDto> tiers = campaign.getPricingTiers().stream()
                .map(t -> new PricingTierDto(t.getMinQuantity(), t.getMaxQuantity(), t.getUnitPrice(), t.getCurrency()))
                .toList();

        return new CampaignResponse(
                campaign.getId(),
                productId,
                productName,
                productSku,
                productBasePrice,
                productImageUrl,
                sellerId,
                sellerName,
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getTargetMinQuantity(),
                campaign.getMaxCampaignQuantity(),
                campaign.getCurrentPaidUnits(),
                campaign.getMinParticipants(),
                campaign.getCurrentParticipants(),
                campaign.getStatus().name(),
                campaign.getStartsAt(),
                campaign.getEndsAt(),
                campaign.getFinalUnitPrice(),
                campaign.getCurrency(),
                tiers,
                campaign.getCreatedAt()
        );
    }
}

