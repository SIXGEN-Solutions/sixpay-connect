package com.sixpay.customer.observation.infrastructure.persistence.protection;

/**
 * Protects reversible projection values and derives deterministic lookup
 * hashes. Implementations must never log plaintext values or keys.
 */
public interface ObservedCustomerDataProtector {

    String protect(String plaintext);

    String reveal(String protectedValue);

    String searchHash(String normalizedValue);
}
