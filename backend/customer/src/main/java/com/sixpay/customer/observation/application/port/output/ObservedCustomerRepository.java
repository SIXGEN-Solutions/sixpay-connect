package com.sixpay.customer.observation.application.port.output;

import com.sixpay.customer.observation.domain.model.ObservedCustomer;

import java.util.Optional;

/**
 * Customer-owned persistence boundary for the Observed Customer projection.
 */
public interface ObservedCustomerRepository {

    Optional<ObservedCustomer> findByNormalizedNiu(
            String normalizedNiu
    );

    void save(ObservedCustomer observedCustomer);
}
