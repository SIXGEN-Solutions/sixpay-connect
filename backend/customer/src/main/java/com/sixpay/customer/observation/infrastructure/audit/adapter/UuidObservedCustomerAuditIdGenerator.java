package com.sixpay.customer.observation.infrastructure.audit.adapter;

import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditIdGenerator;

import java.util.UUID;

public final class UuidObservedCustomerAuditIdGenerator
        implements ObservedCustomerAuditIdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
