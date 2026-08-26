package com.sixpay.customer.verification.application.port.output;

import com.sixpay.customer.verification.domain.event.CustomerVerificationDomainEvent;

import java.util.List;

/**
 * Publishes Customer Verification domain events after persistence.
 */
public interface CustomerVerificationDomainEventPublisher {

    void publish(List<CustomerVerificationDomainEvent> events);
}
