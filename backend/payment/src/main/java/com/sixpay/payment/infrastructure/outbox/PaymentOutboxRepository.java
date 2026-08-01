package com.sixpay.payment.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository
        extends JpaRepository<PaymentOutboxEntity, UUID> {

    @Query(value = """
            SELECT *
              FROM payment_outbox_events
             WHERE (
                    status IN ('PENDING', 'FAILED')
                    AND next_attempt_at <= :now
                   )
                OR (
                    status = 'PROCESSING'
                    AND claimed_at < :staleBefore
                   )
             ORDER BY occurred_at, event_id
             FOR UPDATE SKIP LOCKED
             LIMIT :batchSize
            """, nativeQuery = true)
    List<PaymentOutboxEntity> lockClaimable(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
    );

    boolean existsByEventId(UUID eventId);
}
