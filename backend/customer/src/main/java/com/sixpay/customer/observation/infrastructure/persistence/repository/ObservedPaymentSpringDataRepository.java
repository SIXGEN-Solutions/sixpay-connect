package com.sixpay.customer.observation.infrastructure.persistence.repository;

import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedPaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservedPaymentSpringDataRepository
        extends JpaRepository<ObservedPaymentJpaEntity, UUID> {

    List<ObservedPaymentJpaEntity>
            findByObservedCustomerObservedCustomerIdOrderByPaymentCreatedAtAsc(
                    UUID observedCustomerId
            );
}
