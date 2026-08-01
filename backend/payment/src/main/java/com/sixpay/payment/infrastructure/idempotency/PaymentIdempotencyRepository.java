package com.sixpay.payment.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIdempotencyRepository
        extends JpaRepository<PaymentIdempotencyEntity, UUID> {

    Optional<PaymentIdempotencyEntity>
            findByOperationAndIdempotencyKey(
                    String operation,
                    String idempotencyKey
            );
}
