package com.sixpay.payment.infrastructure.outbox.claim;

import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Claims a bounded batch of Payment outbox events in one short database
 * transaction.
 *
 * <p>The transaction ends before event delivery begins. This prevents calls to
 * downstream modules from holding PostgreSQL row locks.</p>
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

        Objects.requireNonNull(
                transactionManager,
                "transactionManager is required"
        );

        this.transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );
    }

    public List<PaymentOutboxClaim> claimAvailable(
            Instant now,
            Instant staleBefore,
            int batchSize,
            String owner
    ) {
        Objects.requireNonNull(
                now,
                "now is required"
        );
        Objects.requireNonNull(
                staleBefore,
                "staleBefore is required"
        );

        if (staleBefore.isAfter(now)) {
            throw new IllegalArgumentException(
                    "staleBefore must not be after now"
            );
        }

        if (batchSize < 1
                || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "batchSize must be between 1 and "
                            + MAX_BATCH_SIZE
            );
        }

        String normalizedOwner = requireOwner(
                owner
        );

        List<PaymentOutboxClaim> claims =
                transactionTemplate.execute(status -> {
                    List<PaymentOutboxEntity> locked =
                            repository.lockClaimable(
                                    now,
                                    staleBefore,
                                    batchSize
                            );

                    locked.forEach(entity ->
                            entity.claim(
                                    now,
                                    normalizedOwner
                            )
                    );

                    /*
                     * Managed entities would be flushed at commit. An explicit
                     * flush makes the state transition deterministic before
                     * detached claims are returned.
                     */
                    repository.flush();

                    return locked.stream()
                            .map(PaymentOutboxClaim::from)
                            .toList();
                });

        return claims == null
                ? List.of()
                : List.copyOf(claims);
    }

    private static String requireOwner(
            String owner
    ) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException(
                    "owner must not be blank"
            );
        }

        String normalized = owner.strip();

        if (normalized.length() > 100) {
            throw new IllegalArgumentException(
                    "owner must not exceed 100 characters"
            );
        }

        return normalized;
    }
}
