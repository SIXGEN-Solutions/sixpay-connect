package com.sixpay.customer.verification.application.port.output;

import java.util.UUID;

/**
 * Supplies externally generated domain-event identifiers.
 */
@FunctionalInterface
public interface CustomerVerificationEventIdGenerator {

    UUID nextId();
}
