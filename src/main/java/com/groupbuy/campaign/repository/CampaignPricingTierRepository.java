package com.groupbuy.campaign.repository;

import com.groupbuy.campaign.entity.CampaignPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignPricingTierRepository extends JpaRepository<CampaignPricingTier, Long> {

    List<CampaignPricingTier> findByCampaignIdOrderByMinQuantityAsc(Long campaignId);

    void deleteByCampaignId(Long campaignId);
}

