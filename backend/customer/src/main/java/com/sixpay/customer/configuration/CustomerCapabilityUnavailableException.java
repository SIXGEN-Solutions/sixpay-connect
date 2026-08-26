package com.sixpay.customer.configuration;

/**
 * Raised when a Customer capability requires infrastructure that has not
 * been configured.
 *
 * <p>This exception is environment-neutral and prevents missing adapters
 * from being replaced by fabricated banking evidence.</p>
 */
public final class CustomerCapabilityUnavailableException
        extends RuntimeException {

    public CustomerCapabilityUnavailableException(String message) {
        super(message);
    }
}
