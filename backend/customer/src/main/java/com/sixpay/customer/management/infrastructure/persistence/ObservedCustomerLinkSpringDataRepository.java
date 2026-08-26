package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservedCustomerLinkSpringDataRepository
        extends JpaRepository<ObservedCustomerLinkJpaEntity, UUID> {

    List<ObservedCustomerLinkJpaEntity>
            findByCustomerIdAndStatusOrderByLinkedAtDesc(
                    UUID customerId,
                    ObservedCustomerLinkStatus status
            );
}
