package com.sixpay.customer.observation.infrastructure.persistence.adapter;

import com.sixpay.customer.observation.application.port.output.ObservedCustomerIdGenerator;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;

import java.util.UUID;

public final class UuidObservedCustomerIdGenerator
        implements ObservedCustomerIdGenerator {

    @Override
    public ObservedCustomerId nextId() {
        return ObservedCustomerId.of(
                UUID.randomUUID()
        );
    }
}
