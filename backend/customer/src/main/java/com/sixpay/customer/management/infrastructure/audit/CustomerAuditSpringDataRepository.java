package com.sixpay.customer.management.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerAuditSpringDataRepository
        extends JpaRepository<CustomerAuditJpaEntity, UUID> {

    List<CustomerAuditJpaEntity>
            findByAggregateTypeAndAggregateIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                    String aggregateType,
                    UUID aggregateId,
                    Instant from,
                    Instant to
            );
}
