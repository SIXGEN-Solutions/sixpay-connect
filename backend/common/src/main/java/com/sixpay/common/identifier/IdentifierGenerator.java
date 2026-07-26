package com.sixpay.common.identifier;

/**
 * Defines a technology-independent identifier generation contract.
 *
 * @param <T> generated identifier type
 */
@FunctionalInterface
public interface IdentifierGenerator<T> {

    /**
     * Generates a new identifier.
     *
     * @return generated identifier
     */
    T generate();
}