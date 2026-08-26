package com.sixpay.customer.observation.application.port.output;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;

/**
 * Generates technical identifiers for new Observed Customer projections.
 */
@FunctionalInterface
public interface ObservedCustomerIdGenerator {

    ObservedCustomerId nextId();
}
