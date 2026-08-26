package com.sixpay.customer.observation.application.port.input.query;

import com.sixpay.customer.observation.application.query
        .ListObservedCustomerPaymentsQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerPaymentPage;

/**
 * Lists Payment observations linked to one Observed Customer.
 */
@FunctionalInterface
public interface ListObservedCustomerPaymentsUseCase {

    ObservedCustomerPaymentPage listPayments(
            ListObservedCustomerPaymentsQuery query
    );
}
