package com.sixpay.customer.observation.application.port.output.query;

import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPage;

/**
 * Customer-owned read port for linked Payment projection rows.
 */
@FunctionalInterface
public interface ObservedCustomerPaymentQueryRepository {

    ObservedCustomerPaymentPage findByCustomer(
            ListObservedCustomerPaymentsQuery query
    );
}
