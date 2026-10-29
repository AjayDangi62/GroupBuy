package com.groupbuy.campaign.dto;

import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.pricing.dto.PricingQuoteResponse;
import com.groupbuy.pricing.dto.PricingTierDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Detailed campaign response with current qualifying pricing quote")
public record CampaignDetailResponse(
        CampaignResponse campaign,
        @Schema(description = "Current tier quote based on committed paid units so far")
        PricingQuoteResponse currentQuote,
        @Schema(description = "Percent progress towards minimum target quantity (e.g. 70.0%)", example = "70.0")
        double quantityProgressPercent,
        @Schema(description = "Whether the campaign has met its minimum criteria for success")
        boolean targetMet
) {
    public static CampaignDetailResponse of(Campaign campaign, PricingQuoteResponse currentQuote) {
        CampaignResponse summary = CampaignResponse.fromEntity(campaign);
        double progress = 0.0;
        if (campaign.getTargetMinQuantity() > 0) {
            progress = Math.min(100.0, (double) campaign.getCurrentPaidUnits() / campaign.getTargetMinQuantity() * 100.0);
        }
        boolean met = campaign.getCurrentPaidUnits() >= campaign.getTargetMinQuantity() &&
                      campaign.getCurrentParticipants() >= campaign.getMinParticipants();
        return new CampaignDetailResponse(summary, currentQuote, Math.round(progress * 10.0) / 10.0, met);
    }
}

