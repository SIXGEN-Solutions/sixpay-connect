package com.sixpay.payment.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutboxEntity, UUID> {

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
                     WHERE predecessor.aggregate_id = candidate.aggregate_id
                       AND (
                            predecessor.occurred_at < candidate.occurred_at
                            OR (
                                predecessor.occurred_at = candidate.occurred_at
                                AND predecessor.event_id < candidate.event_id
                            )
                           )
                       AND predecessor.status NOT IN ('PUBLISHED', 'DEAD')
                   )
             ORDER BY candidate.occurred_at, candidate.event_id
             FOR UPDATE OF candidate SKIP LOCKED
             LIMIT :batchSize
            """, nativeQuery = true)
    List<PaymentOutboxEntity> lockClaimable(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
    );

    @Query(value = """
            SELECT candidate.*
              FROM payment_outbox_events candidate
             WHERE candidate.event_type = :eventType
               AND (
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
                     WHERE predecessor.aggregate_id = candidate.aggregate_id
                       AND predecessor.event_type = candidate.event_type
                       AND (
                            predecessor.occurred_at < candidate.occurred_at
                            OR (
                                predecessor.occurred_at = candidate.occurred_at
                                AND predecessor.event_id < candidate.event_id
                            )
                           )
                       AND predecessor.status NOT IN ('PUBLISHED', 'DEAD')
                   )
             ORDER BY candidate.occurred_at, candidate.event_id
             FOR UPDATE OF candidate SKIP LOCKED
             LIMIT :batchSize
            """, nativeQuery = true)
    List<PaymentOutboxEntity> lockClaimableByEventType(
            @Param("eventType") String eventType,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
    );

    @Query(value = """
            SELECT COUNT(*)
              FROM payment_outbox_events
             WHERE event_type = :eventType
               AND status NOT IN ('PUBLISHED', 'DEAD')
            """, nativeQuery = true)
    long countOutstandingByEventType(
            @Param("eventType") String eventType
    );

    @Query(value = """
            SELECT MIN(occurred_at)
              FROM payment_outbox_events
             WHERE event_type = :eventType
               AND status NOT IN ('PUBLISHED', 'DEAD')
            """, nativeQuery = true)
    Optional<Instant> findOldestOutstandingOccurredAt(
            @Param("eventType") String eventType
    );

    boolean existsByEventId(UUID eventId);
}
