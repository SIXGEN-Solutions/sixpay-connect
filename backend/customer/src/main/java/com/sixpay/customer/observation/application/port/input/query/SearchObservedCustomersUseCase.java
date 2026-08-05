package com.sixpay.customer.observation.application.port.input.query;

import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;

/**
 * Searches the read-only Observed Customer projection.
 */
@FunctionalInterface
public interface SearchObservedCustomersUseCase {

    ObservedCustomerSearchPage search(
            SearchObservedCustomersQuery query
    );
}
