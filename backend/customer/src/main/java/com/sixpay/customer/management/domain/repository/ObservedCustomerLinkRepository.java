package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservedCustomerLinkRepository {

    ObservedCustomerLink save(ObservedCustomerLink link);

    Optional<ObservedCustomerLink> findByObservedCustomerId(
            UUID observedCustomerId
    );

    List<ObservedCustomerLink> findLinkedByCustomerId(
            CustomerId customerId
    );
}
