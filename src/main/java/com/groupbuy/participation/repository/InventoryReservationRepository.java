package com.groupbuy.participation.repository;

import com.groupbuy.participation.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByParticipationId(Long participationId);

    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'PENDING' AND r.expiresAt <= :now")
    List<InventoryReservation> findExpiredPendingReservations(@Param("now") Instant now);

    List<InventoryReservation> findByCampaignIdAndStatus(Long campaignId, InventoryReservation.Status status);
}

