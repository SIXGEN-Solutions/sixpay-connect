package com.sixpay.customer.observation.infrastructure.audit.adapter;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;
import com.sixpay.customer.observation.infrastructure.audit.entity
        .ObservedCustomerAuditJpaEntity;
import com.sixpay.customer.observation.infrastructure.audit.mapper
        .ObservedCustomerAuditPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.audit.repository
        .ObservedCustomerAuditSpringDataRepository;

import java.util.Objects;

public final class JpaObservedCustomerAuditAdapter
        implements ObservedCustomerAuditPort {

    private final ObservedCustomerAuditSpringDataRepository repository;
    private final ObservedCustomerAuditPersistenceMapper mapper;

    public JpaObservedCustomerAuditAdapter(
            ObservedCustomerAuditSpringDataRepository repository,
            ObservedCustomerAuditPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository is required"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper is required"
        );
    }

    @Override
    public void append(ObservedCustomerAuditRecord record) {
        Objects.requireNonNull(record, "record is required");

        if (repository.existsById(record.auditId())) {
            throw new IllegalStateException(
                    "Observed Customer audit record already exists: "
                            + record.auditId()
            );
        }

        ObservedCustomerAuditJpaEntity entity =
                mapper.toEntity(record);

        repository.saveAndFlush(entity);
    }
}
