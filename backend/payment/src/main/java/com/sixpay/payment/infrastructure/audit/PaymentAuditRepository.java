package com.sixpay.payment.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PaymentAuditRepository extends JpaRepository<PaymentAuditEntity, UUID> {
    List<PaymentAuditEntity> findByPaymentIdOrderByBusinessVersionAscEventSequenceAsc(UUID paymentId);
    boolean existsByEventId(UUID eventId);
}
