package com.groupbuy.participation.repository;

import com.groupbuy.participation.entity.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    List<Participation> findByCampaignId(Long campaignId);

    Page<Participation> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT p FROM Participation p WHERE p.campaign.id = :campaignId AND p.user.id = :userId AND p.status IN ('INITIATED', 'AUTHORIZED', 'CONFIRMED')")
    Optional<Participation> findActiveByCampaignAndUser(@Param("campaignId") Long campaignId, @Param("userId") Long userId);

    List<Participation> findByCampaignIdAndStatus(Long campaignId, Participation.Status status);

    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM Participation p WHERE p.campaign.id = :campaignId AND p.status IN ('AUTHORIZED', 'CONFIRMED')")
    int sumCommittedUnitsByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT COUNT(DISTINCT p.user.id) FROM Participation p WHERE p.campaign.id = :campaignId AND p.status IN ('AUTHORIZED', 'CONFIRMED')")
    int countDistinctCommittedParticipants(@Param("campaignId") Long campaignId);
}

