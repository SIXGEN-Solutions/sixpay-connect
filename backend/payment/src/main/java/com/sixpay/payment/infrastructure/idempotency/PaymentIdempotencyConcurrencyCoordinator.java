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
    public <T> T executeScopedLocked(
            String partnerIdentifier,
            String operation,
            String idempotencyKey,
            Supplier<T> action
    ) {
        if (partnerIdentifier == null || partnerIdentifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Partner identifier is invalid"
            );
        }
        return executeLockKey(
                partnerIdentifier.strip()
                        + ":"
                        + lockKey(operation, idempotencyKey),
                action
        );
    }

    /**
     * Serializes initiation attempts that target the same Partner-owned
     * external Payment identity, independently from the transport
     * idempotency key. This closes the race where two different keys could
     * concurrently observe no Payment and then compete on the database
     * uniqueness constraint.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T executePartnerPaymentReferenceLocked(
            String partnerIdentifier,
            String externalPaymentReference,
            Supplier<T> action
    ) {
        if (partnerIdentifier == null || partnerIdentifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Partner identifier is invalid"
            );
        }
        if (externalPaymentReference == null
                || externalPaymentReference.isBlank()
                || externalPaymentReference.length() > 128) {
            throw new IllegalArgumentException(
                    "External Payment reference is invalid"
            );
        }

        return executeLockKey(
                "PAYMENT_EXTERNAL_REFERENCE:"
                        + partnerIdentifier.strip()
                        + ":"
                        + externalPaymentReference.strip(),
                action
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

        return executeLockKey(lockKey, action);
    }

    private <T> T executeLockKey(
            String lockKey,
            Supplier<T> action
    ) {
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
