package com.sixpay.customer.observation.application.port.output.audit;

import java.util.UUID;

@FunctionalInterface
public interface ObservedCustomerAuditIdGenerator {

    UUID nextId();
}
