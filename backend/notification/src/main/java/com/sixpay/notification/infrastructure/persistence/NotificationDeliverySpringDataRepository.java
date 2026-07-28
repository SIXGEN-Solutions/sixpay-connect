package com.sixpay.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
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
}
