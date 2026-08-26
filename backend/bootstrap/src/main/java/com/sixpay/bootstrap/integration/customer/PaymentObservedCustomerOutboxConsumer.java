package com.sixpay.bootstrap.integration.customer;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.service
        .PaymentObservedCustomerProjectionService;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Consumer invoked by the Payment outbox publication pipeline.
 *
 * <p>The consumed event is the durable Observed Customer projection
 * contract. Outbox acknowledgement must happen only after this consumer
 * returns successfully. Customer replay and stale-event dispositions are
 * considered successful technical consumption outcomes.</p>
 */
public final class PaymentObservedCustomerOutboxConsumer
        implements Consumer<ObservedCustomerProjectionEvent> {

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
    public void accept(
            ObservedCustomerProjectionEvent event
    ) {
        Objects.requireNonNull(
                event,
                "event is required"
        );

        service.project(event);
    }
}