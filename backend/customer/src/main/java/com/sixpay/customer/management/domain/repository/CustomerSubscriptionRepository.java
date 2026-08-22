package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerSubscriptionRepository {

    CustomerSubscription save(CustomerSubscription subscription);

    Optional<CustomerSubscription> findById(
            CustomerSubscriptionId subscriptionId
    );

    List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    );

    boolean existsOpenByCustomerIdAndPartnerId(
            CustomerId customerId,
            UUID partnerId
    );
}
