package com.groupbuy.campaign.repository;

import com.groupbuy.campaign.entity.Campaign;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campaign c WHERE c.id = :id")
    Optional<Campaign> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.pricingTiers WHERE c.id = :id")
    Optional<Campaign> findByIdWithTiers(@Param("id") Long id);

    Page<Campaign> findByStatus(Campaign.Status status, Pageable pageable);

    Page<Campaign> findByProductId(Long productId, Pageable pageable);

    Page<Campaign> findBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.startsAt <= :now AND c.endsAt > :now")
    Page<Campaign> findActiveCampaigns(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'SCHEDULED' AND c.startsAt <= :now")
    List<Campaign> findScheduledCampaignsReadyToStart(@Param("now") Instant now);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.endsAt <= :now")
    List<Campaign> findActiveCampaignsDueForFinalization(@Param("now") Instant now);
}

