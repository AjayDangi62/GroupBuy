package com.groupbuy.payment.repository;

import com.groupbuy.payment.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    List<PaymentEvent> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}

