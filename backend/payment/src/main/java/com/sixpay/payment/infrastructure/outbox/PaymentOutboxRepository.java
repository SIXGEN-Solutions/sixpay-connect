package com.sixpay.payment.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutboxEntity, UUID> {

    /**
     * Locks a bounded set of claimable outbox rows.
     *
     * <p>The predecessor condition enforces head-of-line processing for each
     * Payment aggregate. An event is claimable only when no older unpublished
     * event exists for the same aggregate. Different aggregates remain
     * independently claimable and can therefore be processed concurrently.</p>
     */
    @Query(value = """
            SELECT candidate.*
              FROM payment_outbox_events candidate
             WHERE (
                    (
                        candidate.status IN ('PENDING', 'FAILED')
                        AND candidate.next_attempt_at <= :now
                    )
                    OR (
                        candidate.status = 'PROCESSING'
                        AND candidate.claimed_at < :staleBefore
                    )
                   )
               AND NOT EXISTS (
                    SELECT 1
                      FROM payment_outbox_events predecessor
                     WHERE predecessor.aggregate_id =
                               candidate.aggregate_id
                       AND (
                            predecessor.occurred_at <
                                candidate.occurred_at
                            OR (
                                predecessor.occurred_at =
                                    candidate.occurred_at
                                AND predecessor.event_id <
                                    candidate.event_id
                            )
                           )
                       AND predecessor.status NOT IN (
                            'PUBLISHED',
                            'DEAD'
                       )
                   )
             ORDER BY candidate.occurred_at,
                      candidate.event_id
             FOR UPDATE OF candidate SKIP LOCKED
             LIMIT :batchSize
            """, nativeQuery = true)
    List<PaymentOutboxEntity> lockClaimable(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
    );

    boolean existsByEventId(UUID eventId);
}
