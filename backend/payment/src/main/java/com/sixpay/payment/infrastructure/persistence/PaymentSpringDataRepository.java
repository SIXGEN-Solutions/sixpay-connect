package com.sixpay.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentSpringDataRepository
        extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByPublicPaymentReference(
            String publicPaymentReference
    );

    Optional<PaymentJpaEntity>
            findByCanonicalPartnerIdAndExternalPaymentReference(
                    UUID canonicalPartnerId,
                    String externalPaymentReference
            );

    boolean existsByCanonicalPartnerIdAndExternalPaymentReference(
            UUID canonicalPartnerId,
            String externalPaymentReference
    );
}
