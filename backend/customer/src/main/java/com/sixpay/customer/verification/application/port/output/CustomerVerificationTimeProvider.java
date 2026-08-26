package com.sixpay.customer.verification.application.port.output;

import java.time.Instant;

/**
 * Supplies application time explicitly to domain operations.
 */
@FunctionalInterface
public interface CustomerVerificationTimeProvider {

    Instant now();
}
