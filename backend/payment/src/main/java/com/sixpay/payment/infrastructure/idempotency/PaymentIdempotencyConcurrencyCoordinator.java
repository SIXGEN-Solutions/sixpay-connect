package com.sixpay.payment.infrastructure.idempotency;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Serializes concurrent requests for the same Payment operation and
 * idempotency key using a PostgreSQL transaction-scoped advisory lock.
 */
@Component
public class PaymentIdempotencyConcurrencyCoordinator {

    private final EntityManager entityManager;

    public PaymentIdempotencyConcurrencyCoordinator(
            EntityManager entityManager
    ) {
        this.entityManager = Objects.requireNonNull(
                entityManager,
                "Entity manager"
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T executeLocked(
            String operation,
            String idempotencyKey,
            Supplier<T> action
    ) {
        String lockKey = lockKey(
                operation,
                idempotencyKey
        );

        entityManager.createNativeQuery(
                        """
                        SELECT pg_advisory_xact_lock(
                            hashtextextended(
                                CAST(:lockKey AS text),
                                0
                            )
                        )
                        """
                )
                .setParameter("lockKey", lockKey)
                .getSingleResult();

        return Objects.requireNonNull(
                action,
                "Idempotency locked action"
        ).get();
    }

    private static String lockKey(
            String operation,
            String idempotencyKey
    ) {
        if (operation == null
                || operation.isBlank()
                || operation.length() > 160) {
            throw new IllegalArgumentException(
                    "Idempotency operation is invalid"
            );
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 150) {
            throw new IllegalArgumentException(
                    "Idempotency key is invalid"
            );
        }

        return operation + ":" + idempotencyKey;
    }
}
