package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerSubscriptionUseCase {

    CustomerSubscription create(
            CustomerId customerId,
            UUID partnerId,
            com.sixpay.customer.management.domain.model.CustomerBankAccountId bankAccountId,
            Instant now
    );

    CustomerSubscription activate(
            CustomerSubscriptionId subscriptionId,
            Instant now
    );

    CustomerSubscription suspend(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    );

    CustomerSubscription close(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    );

    CustomerSubscription findById(
            CustomerSubscriptionId subscriptionId
    );

    List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    );
}
