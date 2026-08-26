package com.sixpay.common.identifier;

import java.util.UUID;

/**
 * Generates identifiers using UUID.
 */
public final class UuidIdentifierGenerator
        implements IdentifierGenerator<UUID> {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}