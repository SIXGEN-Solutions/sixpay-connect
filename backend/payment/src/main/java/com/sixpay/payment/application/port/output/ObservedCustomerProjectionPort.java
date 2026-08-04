package com.sixpay.payment.application.port.output;

/**
 * Payment-owned boundary used to project one durable Payment event into the
 * Observed Customer capability.
 */
public interface ObservedCustomerProjectionPort {

    ObservedCustomerProjectionResult project(
            ObservedCustomerProjectionRequest request
    );
}
