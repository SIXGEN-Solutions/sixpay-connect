package com.sixpay.customer.observation.infrastructure.persistence.adapter;

import com.sixpay.customer.observation.application.port.output.ObservedCustomerRepository;
import com.sixpay.customer.observation.domain.model.ObservedCustomer;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.mapper.ObservedCustomerPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.persistence.protection.ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedCustomerSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedPaymentSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ProcessedObservationEventSpringDataRepository;

import java.util.Objects;
import java.util.Optional;

public final class JpaObservedCustomerRepositoryAdapter
        implements ObservedCustomerRepository {

    private final ObservedCustomerSpringDataRepository customers;
    private final ObservedPaymentSpringDataRepository payments;
    private final ProcessedObservationEventSpringDataRepository events;
    private final ObservedCustomerDataProtector protector;
    private final ObservedCustomerPersistenceMapper mapper;

    public JpaObservedCustomerRepositoryAdapter(
            ObservedCustomerSpringDataRepository customers,
            ObservedPaymentSpringDataRepository payments,
            ProcessedObservationEventSpringDataRepository events,
            ObservedCustomerDataProtector protector,
            ObservedCustomerPersistenceMapper mapper
    ) {
        this.customers = Objects.requireNonNull(
                customers,
                "customers is required"
        );
        this.payments = Objects.requireNonNull(
                payments,
                "payments is required"
        );
        this.events = Objects.requireNonNull(
                events,
                "events is required"
        );
        this.protector = Objects.requireNonNull(
                protector,
                "protector is required"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper is required"
        );
    }

    @Override
    public Optional<ObservedCustomer> findByNormalizedNiu(
            String normalizedNiu
    ) {
        String searchHash =
                protector.searchHash(normalizedNiu);

        return customers.findByNiuSearchHash(searchHash)
                .map(entity ->
                        mapper.toDomain(
                                entity,
                                payments
                                        .findByObservedCustomerObservedCustomerIdOrderByPaymentCreatedAtAsc(
                                                entity.getObservedCustomerId()
                                        ),
                                events
                                        .findByObservedCustomerObservedCustomerId(
                                                entity.getObservedCustomerId()
                                        )
                        )
                );
    }

    @Override
    public void save(ObservedCustomer observedCustomer) {
        ObservedCustomerJpaEntity entity =
                customers.findById(
                        observedCustomer.id().value()
                ).orElseGet(
                        ObservedCustomerJpaEntity::new
                );

        mapper.copyToEntity(
                observedCustomer,
                entity
        );
        customers.save(entity);
    }
}
