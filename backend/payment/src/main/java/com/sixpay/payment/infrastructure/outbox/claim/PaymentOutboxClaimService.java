package com.sixpay.payment.infrastructure.outbox.claim;

import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Claims a bounded batch in one short database transaction.
 */
@Component
public final class PaymentOutboxClaimService {

    private static final int MAX_BATCH_SIZE = 500;

    private final PaymentOutboxRepository repository;
    private final TransactionTemplate transactionTemplate;

    public PaymentOutboxClaimService(
            PaymentOutboxRepository repository,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository is required"
        );
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(
                        transactionManager,
                        "transactionManager is required"
                )
        );
    }

    public List<PaymentOutboxClaim> claimAvailable(
            Instant now,
            Instant staleBefore,
            int batchSize,
            String owner
    ) {
        validate(now, staleBefore, batchSize, owner);
        return claim(
                now,
                owner,
                () -> repository.lockClaimable(
                        now,
                        staleBefore,
                        batchSize
                )
        );
    }

    public List<PaymentOutboxClaim> claimAvailableByEventType(
            String eventType,
            Instant now,
            Instant staleBefore,
            int batchSize,
            String owner
    ) {
        String normalizedType = requireText(eventType, "eventType");
        validate(now, staleBefore, batchSize, owner);

        return claim(
                now,
                owner,
                () -> repository.lockClaimableByEventType(
                        normalizedType,
                        now,
                        staleBefore,
                        batchSize
                )
        );
    }

    private List<PaymentOutboxClaim> claim(
            Instant now,
            String owner,
            Supplier<List<PaymentOutboxEntity>> loader
    ) {
        String normalizedOwner = requireText(owner, "owner");

        List<PaymentOutboxClaim> claims = transactionTemplate.execute(status -> {
            List<PaymentOutboxEntity> locked = loader.get();
            locked.forEach(entity -> entity.claim(now, normalizedOwner));
            repository.flush();
            return locked.stream()
                    .map(PaymentOutboxClaim::from)
                    .toList();
        });

        return claims == null ? List.of() : List.copyOf(claims);
    }

    private static void validate(
            Instant now,
            Instant staleBefore,
            int batchSize,
            String owner
    ) {
        Objects.requireNonNull(now, "now is required");
        Objects.requireNonNull(staleBefore, "staleBefore is required");
        requireText(owner, "owner");

        if (staleBefore.isAfter(now)) {
            throw new IllegalArgumentException(
                    "staleBefore must not be after now"
            );
        }

        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "batchSize must be between 1 and " + MAX_BATCH_SIZE
            );
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.strip();
        if (normalized.length() > 150) {
            throw new IllegalArgumentException(
                    field + " must not exceed 150 characters"
            );
        }
        return normalized;
    }
}
