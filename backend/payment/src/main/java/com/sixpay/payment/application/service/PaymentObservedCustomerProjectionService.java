package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.event.PaymentDomainEvent;

import java.util.Objects;

/**
 * Handles one durable Payment domain event for the Observed Customer
 * projection.
 */
public final class PaymentObservedCustomerProjectionService {

    private final PaymentLookupPort paymentLookupPort;
    private final ObservedCustomerProjectionPort projectionPort;
    private final PaymentObservedCustomerProjectionRequestFactory
            requestFactory;

    public PaymentObservedCustomerProjectionService(
            PaymentLookupPort paymentLookupPort,
            ObservedCustomerProjectionPort projectionPort,
            PaymentObservedCustomerProjectionRequestFactory requestFactory
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "paymentLookupPort is required"
        );
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
            PaymentDomainEvent event
    ) {
        Objects.requireNonNull(event, "event is required");

        var payment = paymentLookupPort
                .findById(event.paymentId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Payment not found for durable event "
                                        + event.eventId()
                        )
                );

        return projectionPort.project(
                requestFactory.from(payment, event)
        );
    }
}
