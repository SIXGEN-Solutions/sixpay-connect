package com.sixpay.customer.observation.infrastructure.persistence.repository;

import com.sixpay.customer.observation.infrastructure.persistence.entity.ObservedCustomerJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ObservedCustomerSpringDataRepository
        extends JpaRepository<ObservedCustomerJpaEntity, UUID> {

    @EntityGraph(
            attributePaths = {
                    "institutions",
                    "institutions.accounts"
            }
    )
    Optional<ObservedCustomerJpaEntity> findByNiuSearchHash(
            String niuSearchHash
    );

    @EntityGraph(
            attributePaths = {
                    "institutions",
                    "institutions.accounts"
            }
    )
    Optional<ObservedCustomerJpaEntity>
            findDetailedByObservedCustomerId(
                    UUID observedCustomerId
            );
}
