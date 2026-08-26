package com.sixpay.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface NotificationDeliverySpringDataRepository
        extends JpaRepository<NotificationDeliveryJpaEntity, UUID> {

    Optional<NotificationDeliveryJpaEntity> findByEventId(UUID eventId);

    @Modifying
    @Query(value = """
            INSERT INTO sixpay.notification_deliveries (
                id,
                event_id,
                aggregate_id,
                event_type,
                recipient,
                template,
                reason,
                status,
                attempt_count,
                next_attempt_at,
                last_error,
                created_at,
                sent_at,
                correlation_id
            )
            VALUES (
                :id,
                :eventId,
                :aggregateId,
                :eventType,
                :recipient,
                :template,
                :reason,
                'PROCESSING',
                1,
                NULL,
                NULL,
                :createdAt,
                NULL,
                :correlationId
            )
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertProcessing(
            @Param("id") UUID id,
            @Param("eventId") UUID eventId,
            @Param("aggregateId") UUID aggregateId,
            @Param("eventType") String eventType,
            @Param("recipient") String recipient,
            @Param("template") String template,
            @Param("reason") String reason,
            @Param("createdAt") Instant createdAt,
            @Param("correlationId") String correlationId
    );

    @Modifying
    @Query(value = """
            UPDATE sixpay.notification_deliveries
               SET status = 'SENT',
                   sent_at = :sentAt,
                   next_attempt_at = NULL,
                   last_error = NULL
             WHERE event_id = :eventId
               AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markSent(
            @Param("eventId") UUID eventId,
            @Param("sentAt") Instant sentAt
    );

    @Modifying
    @Query(value = """
            UPDATE sixpay.notification_deliveries
               SET status = 'FAILED',
                   last_error = :error,
                   next_attempt_at = :nextAttemptAt
             WHERE event_id = :eventId
               AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markFailed(
            @Param("eventId") UUID eventId,
            @Param("error") String error,
            @Param("nextAttemptAt") Instant nextAttemptAt
    );

    @Query(value = """
            SELECT *
              FROM sixpay.notification_deliveries
             WHERE status IN ('PENDING', 'FAILED')
               AND next_attempt_at <= :now
             ORDER BY next_attempt_at, created_at
             LIMIT :batchSize
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationDeliveryJpaEntity> lockDue(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query(value = """
            UPDATE sixpay.notification_deliveries
               SET status = 'DEAD',
                   last_error = :error,
                   next_attempt_at = NULL
             WHERE event_id = :eventId
               AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markDead(
            @Param("eventId") UUID eventId,
            @Param("error") String error
    );
}
