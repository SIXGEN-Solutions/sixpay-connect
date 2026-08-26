package com.sixpay.customer.observation.infrastructure.persistence.repository;

import com.sixpay.customer.observation.infrastructure.persistence.entity.ProcessedObservationEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessedObservationEventSpringDataRepository
        extends JpaRepository<
                ProcessedObservationEventJpaEntity,
                UUID
        > {

    List<ProcessedObservationEventJpaEntity>
            findByObservedCustomerObservedCustomerId(
                    UUID observedCustomerId
            );
}
