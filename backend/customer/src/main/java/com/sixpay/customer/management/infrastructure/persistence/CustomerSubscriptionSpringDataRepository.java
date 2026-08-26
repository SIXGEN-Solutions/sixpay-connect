package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CustomerSubscriptionSpringDataRepository
        extends JpaRepository<CustomerSubscriptionJpaEntity, UUID> {

    List<CustomerSubscriptionJpaEntity> findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId
    );

    boolean existsByCustomerIdAndPartnerIdAndStatusIn(
            UUID customerId,
            UUID partnerId,
            Collection<CustomerSubscriptionStatus> statuses
    );
}
