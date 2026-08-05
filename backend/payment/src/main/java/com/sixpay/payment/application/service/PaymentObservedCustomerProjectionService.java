package com.sixpay.payment.application.service;

import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;

import java.util.Objects;

/**
 * Projects one durable Payment snapshot into the Observed Customer capability.
 * The current Payment aggregate is never reloaded.
 */
public final class PaymentObservedCustomerProjectionService {

    private final ObservedCustomerProjectionPort projectionPort;
    private final PaymentObservedCustomerProjectionRequestFactory requestFactory;

    public PaymentObservedCustomerProjectionService(
            ObservedCustomerProjectionPort projectionPort,
            PaymentObservedCustomerProjectionRequestFactory requestFactory
    ) {
        this.projectionPort = Objects.requireNonNull(
                projectionPort,
                "projectionPort is required"
        );
        this.requestFactory = Objects.requireNonNull(
                requestFactory,
                "requestFactory is required"
        );
    }

    public ObservedCustomerProjectionResult project(
            ObservedCustomerProjectionEvent event
    ) {
        Objects.requireNonNull(event, "event is required");

        ObservedCustomerProjectionResult result =
                projectionPort.project(requestFactory.from(event));

        if (!event.eventId().equals(result.sourceEventId())) {
            throw new IllegalStateException(
                    "Observed Customer projection returned a different sourceEventId"
            );
        }

        return result;
    }
}
