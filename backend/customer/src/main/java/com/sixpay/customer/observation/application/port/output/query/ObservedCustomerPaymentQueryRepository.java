package com.sixpay.customer.observation.application.port.output.query;

import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentSlice;

/**
 * Dedicated read port for Payment observations linked to one customer.
 */
@FunctionalInterface
public interface ObservedCustomerPaymentQueryRepository {

    ObservedCustomerPaymentSlice findByCustomerId(
            ObservedCustomerPaymentCriteria criteria
    );
}
