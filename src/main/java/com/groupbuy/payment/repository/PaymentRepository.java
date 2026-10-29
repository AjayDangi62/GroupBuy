package com.groupbuy.payment.repository;

import com.groupbuy.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByParticipationId(Long participationId);

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    List<Payment> findByParticipationCampaignIdAndStatus(Long campaignId, Payment.Status status);
}

