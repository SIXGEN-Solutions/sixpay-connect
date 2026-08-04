package com.sixpay.bootstrap.integration.customer;

import com.sixpay.payment.application.service.PaymentObservedCustomerProjectionService;
import com.sixpay.payment.domain.event.PaymentDomainEvent;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Consumer invoked by the Payment outbox publication pipeline.
 *
 * <p>Outbox acknowledgement must happen only after this consumer returns
 * successfully. A replay is considered successful because Customer
 * Observation enforces source-event idempotence.</p>
 */
public final class PaymentObservedCustomerOutboxConsumer
        implements Consumer<PaymentDomainEvent> {

    private final PaymentObservedCustomerProjectionService service;

    public PaymentObservedCustomerOutboxConsumer(
            PaymentObservedCustomerProjectionService service
    ) {
        this.service = Objects.requireNonNull(
                service,
                "service is required"
        );
    }

    @Override
    public void accept(PaymentDomainEvent event) {
        service.project(event);
    }
}
