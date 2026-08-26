package com.sixpay.customer.management.infrastructure.audit;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;
import com.sixpay.customer.management.application.port.output.CustomerAuditTrail;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class CustomerAuditTrailAdapter
        implements CustomerAuditTrail {

    private final CustomerAuditSpringDataRepository repository;

    public CustomerAuditTrailAdapter(
            CustomerAuditSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void append(CustomerAuditRecord record) {
        repository.save(
                new CustomerAuditJpaEntity(record)
        );
    }

    @Override
    public List<CustomerAuditRecord> find(
            String aggregateType,
            UUID aggregateId,
            Instant from,
            Instant to
    ) {
        return repository
                .findByAggregateTypeAndAggregateIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        aggregateType,
                        aggregateId,
                        from,
                        to
                )
                .stream()
                .map(CustomerAuditJpaEntity::toRecord)
                .toList();
    }
}
