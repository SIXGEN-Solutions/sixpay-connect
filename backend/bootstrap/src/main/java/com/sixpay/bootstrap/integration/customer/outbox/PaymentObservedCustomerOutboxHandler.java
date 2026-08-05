package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;
import com.sixpay.payment.application.service.PaymentObservedCustomerProjectionService;

import java.util.Objects;

/**
 * Handles one deserialized Observed Customer projection event.
 */
public final class PaymentObservedCustomerOutboxHandler {

    private final PaymentObservedCustomerProjectionService projectionService;

    public PaymentObservedCustomerOutboxHandler(
            PaymentObservedCustomerProjectionService projectionService
    ) {
        this.projectionService = Objects.requireNonNull(
                projectionService,
                "projectionService is required"
        );
    }

    public ObservedCustomerProjectionResult handle(
            ObservedCustomerProjectionEvent event
    ) {
        Objects.requireNonNull(event, "event is required");

        ObservedCustomerProjectionResult result =
                projectionService.project(event);

        return switch (result.disposition()) {
            case APPLIED, REPLAYED, IGNORED_STALE -> result;
        };
    }
}
