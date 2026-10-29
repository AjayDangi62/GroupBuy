package com.groupbuy.payment.repository;

import com.groupbuy.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(Long paymentId);
}

