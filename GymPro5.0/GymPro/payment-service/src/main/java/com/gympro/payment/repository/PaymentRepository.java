package com.gympro.payment.repository;

import com.gympro.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByMemberId(Long memberId);
    List<Payment> findByStatus(String status);
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findBySubscriptionId(Long subscriptionId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByRefundStatus(String refundStatus);
}
