package com.sixpay.customer.observation.infrastructure.persistence.adapter;

import com.sixpay.customer.observation.application.port.output.ObservedPaymentRepository;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentReference;
import com.sixpay.customer.observation.domain.model.ProjectionWatermark;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedPaymentJpaEntity;
import com.sixpay.customer.observation.infrastructure.persistence.mapper.ObservedCustomerPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedCustomerSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ObservedPaymentSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository.ProcessedObservationEventSpringDataRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class JpaObservedPaymentRepositoryAdapter
        implements ObservedPaymentRepository {

    private final ObservedCustomerSpringDataRepository customers;
    private final ObservedPaymentSpringDataRepository payments;
    private final ProcessedObservationEventSpringDataRepository events;
    private final ObservedCustomerPersistenceMapper mapper;

    public JpaObservedPaymentRepositoryAdapter(
            ObservedCustomerSpringDataRepository customers,
            ObservedPaymentSpringDataRepository payments,
            ProcessedObservationEventSpringDataRepository events,
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
        this.mapper = Objects.requireNonNull(
                mapper,
                "mapper is required"
        );
    }

    @Override
    public void save(
            ObservedCustomerId observedCustomerId,
            UUID sourceEventId,
            ObservedPaymentReference payment,
            ProjectionWatermark watermark,
            Instant observedAt
    ) {
        if (events.existsById(sourceEventId)) {
            return;
        }

        ObservedCustomerJpaEntity customer =
                customers.getReferenceById(
                        observedCustomerId.value()
                );

        ObservedPaymentJpaEntity paymentEntity =
                payments.findById(payment.paymentId())
                        .orElseGet(
                                ObservedPaymentJpaEntity::new
                        );

        mapper.copyPayment(
                payment,
                customer,
                paymentEntity
        );
        payments.save(paymentEntity);

        events.save(
                mapper.toEventEntity(
                        sourceEventId,
                        customer,
                        watermark,
                        observedAt
                )
        );
    }
}
