package com.sixpay.customer.observation.application.port.output.query;

import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchPage;
import com.sixpay.customer.observation.application.query
        .SearchObservedCustomersQuery;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;

import java.util.Optional;

/**
 * Customer-owned read port backed by a dedicated projection query adapter.
 *
 * <p>Implementations must query projection rows directly and must not
 * reconstitute the mutable ObservedCustomer aggregate.</p>
 */
public interface ObservedCustomerQueryRepository {

    ObservedCustomerSearchPage search(
            SearchObservedCustomersQuery query
    );

    Optional<ObservedCustomerDetailView> findDetailById(
            ObservedCustomerId observedCustomerId
    );

    boolean existsById(
            ObservedCustomerId observedCustomerId
    );
}
