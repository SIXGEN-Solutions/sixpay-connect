package com.sixpay.partner.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventSpringDataRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = """
            SELECT *
              FROM partner_outbox_events
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
    List<OutboxEventJpaEntity> lockClaimable(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize
    );
}
