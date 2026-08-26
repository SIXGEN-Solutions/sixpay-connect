package com.sixpay.customer.observation.application.port.input.query;

import com.sixpay.customer.observation.application.query
        .GetObservedCustomerQuery;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;

/**
 * Reads one Observed Customer detail projection.
 */
@FunctionalInterface
public interface GetObservedCustomerUseCase {

    ObservedCustomerDetailView get(
            GetObservedCustomerQuery query
    );
}
