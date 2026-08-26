package com.sixpay.payment.infrastructure.outbox.claim;

import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOutboxClaimServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-08-04T18:00:00Z"
            );

    @Test
    void claimsLockedRowsAndReturnsDetachedSafeModels() {
        PaymentOutboxRepository repository =
                mock(
                        PaymentOutboxRepository.class
                );

        PaymentOutboxEntity entity = entity(
                "11111111-1111-4111-8111-111111111111",
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
                NOW.minusSeconds(5)
        );

        when(
                repository.lockClaimable(
                        NOW,
                        NOW.minusSeconds(120),
                        10
                )
        ).thenReturn(
                List.of(entity)
        );

        PaymentOutboxClaimService service =
                new PaymentOutboxClaimService(
                        repository,
                        transactionManager()
                );

        List<PaymentOutboxClaim> claims =
                service.claimAvailable(
                        NOW,
                        NOW.minusSeconds(120),
                        10,
                        "worker-a"
                );

        assertEquals(
                1,
                claims.size()
        );

        assertEquals(
                PaymentOutboxEntity.Status.PROCESSING,
                entity.status()
        );

        assertEquals(
                1,
                entity.attemptCount()
        );

        assertEquals(
                "worker-a",
                entity.claimedBy()
        );

        assertEquals(
                "worker-a",
                claims.getFirst().claimedBy()
        );

        assertFalse(
                claims.getFirst()
                        .toString()
                        .contains(
                                "\"normalizedNiu\""
                        )
        );

        assertFalse(
                claims.getFirst()
                        .toString()
                        .contains(
                                "{\"safe\":true}"
                        )
        );

        verify(repository).flush();
    }

    @Test
    void validatesBoundedClaimArguments() {
        PaymentOutboxClaimService service =
                new PaymentOutboxClaimService(
                        mock(
                                PaymentOutboxRepository.class
                        ),
                        transactionManager()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.claimAvailable(
                        NOW,
                        NOW.plusSeconds(1),
                        10,
                        "worker"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.claimAvailable(
                        NOW,
                        NOW.minusSeconds(1),
                        0,
                        "worker"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.claimAvailable(
                        NOW,
                        NOW.minusSeconds(1),
                        501,
                        "worker"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.claimAvailable(
                        NOW,
                        NOW.minusSeconds(1),
                        10,
                        " "
                )
        );
    }

    private static PaymentOutboxEntity entity(
            String eventId,
            String aggregateId,
            Instant occurredAt
    ) {
        return PaymentOutboxEntity.create(
                UUID.fromString(
                        eventId
                ),
                UUID.fromString(
                        aggregateId
                ),
                "payment.observation-projection",
                1,
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                "{\"safe\":true}",
                occurredAt,
                occurredAt
        );
    }

    private static PlatformTransactionManager
    transactionManager() {

        return new PlatformTransactionManager() {

            @Override
            public TransactionStatus getTransaction(
                    TransactionDefinition definition
            ) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(
                    TransactionStatus status
            ) {
                // No-op test transaction.
            }

            @Override
            public void rollback(
                    TransactionStatus status
            ) {
                // No-op test transaction.
            }
        };
    }
}