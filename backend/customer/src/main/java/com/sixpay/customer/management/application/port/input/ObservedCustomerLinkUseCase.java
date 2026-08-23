package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservedCustomerLinkUseCase {

    ObservedCustomerLink link(
            UUID observedCustomerId,
            CustomerId customerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    );

    ObservedCustomerLink unlink(
            UUID observedCustomerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    );

    Optional<ObservedCustomerLink> findLinked(
            UUID observedCustomerId
    );

    List<ObservedCustomerLink> findByCustomerId(
            CustomerId customerId
    );
}
