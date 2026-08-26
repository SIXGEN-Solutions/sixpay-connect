package com.sixpay.customer.observation.application.port.input;

/**
 * Stable Customer-owned input boundary used to feed the Observed Customer
 * projection.
 */
public interface ObserveCustomerUseCase {

    ObserveCustomerResult observe(
            ObserveCustomerCommand command
    );
}
