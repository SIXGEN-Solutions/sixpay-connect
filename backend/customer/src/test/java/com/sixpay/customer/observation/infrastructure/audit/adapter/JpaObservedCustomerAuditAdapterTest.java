package com.sixpay.customer.observation.infrastructure.audit.adapter;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditAction;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditContext;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditOutcome;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.infrastructure.audit.entity
        .ObservedCustomerAuditJpaEntity;
import com.sixpay.customer.observation.infrastructure.audit.mapper
        .ObservedCustomerAuditPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.audit.repository
        .ObservedCustomerAuditSpringDataRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaObservedCustomerAuditAdapterTest {

    @Test
    void appendsMappedEntityAndFlushesImmediately() {
        ObservedCustomerAuditSpringDataRepository repository =
                mock(
                        ObservedCustomerAuditSpringDataRepository.class
                );
        ObservedCustomerAuditPersistenceMapper mapper =
                mock(
                        ObservedCustomerAuditPersistenceMapper.class
                );

        ObservedCustomerAuditRecord record = record();
        ObservedCustomerAuditJpaEntity entity =
                ObservedCustomerAuditJpaEntity.create(
                        record.auditId(),
                        record.action(),
                        record.outcome(),
                        null,
                        null,
                        null,
                        record.actorId(),
                        record.correlationId(),
                        record.reasonCode(),
                        record.occurredAt(),
                        1
                );

        when(repository.existsById(record.auditId()))
                .thenReturn(false);
        when(mapper.toEntity(record))
                .thenReturn(entity);

        new JpaObservedCustomerAuditAdapter(
                repository,
                mapper
        ).append(record);

        verify(repository).existsById(record.auditId());
        verify(mapper).toEntity(record);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void duplicateAuditIdIsRejectedWithoutSave() {
        ObservedCustomerAuditSpringDataRepository repository =
                mock(
                        ObservedCustomerAuditSpringDataRepository.class
                );
        ObservedCustomerAuditPersistenceMapper mapper =
                mock(
                        ObservedCustomerAuditPersistenceMapper.class
                );

        ObservedCustomerAuditRecord record = record();

        when(repository.existsById(record.auditId()))
                .thenReturn(true);

        JpaObservedCustomerAuditAdapter adapter =
                new JpaObservedCustomerAuditAdapter(
                        repository,
                        mapper
                );

        assertThrows(
                IllegalStateException.class,
                () -> adapter.append(record)
        );

        verify(mapper, never()).toEntity(record);
        verify(repository, never())
                .saveAndFlush(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    private static ObservedCustomerAuditRecord record() {
        return ObservedCustomerAuditRecord.query(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                ObservedCustomerAuditAction.QUERY_SEARCHED,
                ObservedCustomerAuditOutcome.SUCCEEDED,
                null,
                new ObservedCustomerAuditContext(
                        "service-account:customer",
                        "55555555-5555-4555-8555-555555555555"
                ),
                Instant.parse("2026-08-05T15:00:00Z"),
                null
        );
    }
}
