package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.PaymentSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentSpringDataRepository
        extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByPublicPaymentReference(
            String publicPaymentReference
    );

    Optional<PaymentJpaEntity>
            findBySourceAndExternalPaymentReference(
                    PaymentSource source,
                    String externalPaymentReference
            );

    boolean existsBySourceAndExternalPaymentReference(
            PaymentSource source,
            String externalPaymentReference
    );
}
