package com.sixpay.notification.infrastructure.operational.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OperationalNotificationSpringDataRepository
        extends JpaRepository<
                OperationalNotificationJpaEntity,
                UUID
        > {

    Optional<OperationalNotificationJpaEntity>
    findByDeduplicationKey(
            String deduplicationKey
    );

    @Query(
            value = """
                    select notification_id
                    from sixpay.operational_notification_deliveries
                    where status in ('PENDING', 'FAILED_RETRYABLE')
                      and next_attempt_at <= :dueAt
                    order by next_attempt_at asc, created_at asc
                    limit :batchSize
                    """,
            nativeQuery = true
    )
    List<UUID> findDueNotificationIds(
            @Param("dueAt") Instant dueAt,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query(
            value = """
                    update sixpay.operational_notification_deliveries
                    set status = 'DISPATCHING',
                        attempt_count = attempt_count + 1,
                        last_attempt_at = :claimedAt,
                        next_attempt_at = null,
                        last_error_code = null,
                        version = version + 1
                    where notification_id = :notificationId
                      and status in ('PENDING', 'FAILED_RETRYABLE')
                      and next_attempt_at <= :claimedAt
                    """,
            nativeQuery = true
    )
    int claimForDispatch(
            @Param("notificationId") UUID notificationId,
            @Param("claimedAt") Instant claimedAt
    );

    @Modifying
    @Query(
            value = """
                    insert into sixpay.operational_notification_deliveries (
                        notification_id,
                        trigger_type,
                        source_id,
                        recipient_type,
                        recipient_reference,
                        recipient_locale,
                        channel,
                        template_key,
                        deduplication_key,
                        template_variables,
                        status,
                        attempt_count,
                        next_attempt_at,
                        created_at,
                        correlation_id,
                        version
                    ) values (
                        :notificationId,
                        :triggerType,
                        :sourceId,
                        :recipientType,
                        :recipientReference,
                        :recipientLocale,
                        :channel,
                        :templateKey,
                        :deduplicationKey,
                        :templateVariables,
                        'PENDING',
                        0,
                        :createdAt,
                        :createdAt,
                        :correlationId,
                        0
                    )
                    on conflict (deduplication_key) do nothing
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("notificationId") UUID notificationId,
            @Param("triggerType") String triggerType,
            @Param("sourceId") String sourceId,
            @Param("recipientType") String recipientType,
            @Param("recipientReference") String recipientReference,
            @Param("recipientLocale") String recipientLocale,
            @Param("channel") String channel,
            @Param("templateKey") String templateKey,
            @Param("deduplicationKey") String deduplicationKey,
            @Param("templateVariables") String templateVariables,
            @Param("createdAt") Instant createdAt,
            @Param("correlationId") String correlationId
    );
}
