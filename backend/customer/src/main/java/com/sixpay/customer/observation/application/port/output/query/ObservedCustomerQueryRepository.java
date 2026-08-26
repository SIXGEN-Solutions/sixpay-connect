package com.sixpay.customer.observation.application.port.output.query;

import com.sixpay.customer.observation.application.query
        .ObservedCustomerDetailView;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchCriteria;
import com.sixpay.customer.observation.application.query
        .ObservedCustomerSearchSlice;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;

import java.util.Optional;

/**
 * Dedicated read port for the Observed Customer projection.
 */
public interface ObservedCustomerQueryRepository {

    ObservedCustomerSearchSlice search(
            ObservedCustomerSearchCriteria criteria
    );

    Optional<ObservedCustomerDetailView> findDetailById(
            ObservedCustomerId observedCustomerId
    );

    boolean existsById(
            ObservedCustomerId observedCustomerId
    );
}
