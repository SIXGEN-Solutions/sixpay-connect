package com.sixpay.common.identifier;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UuidIdentifierGeneratorTest {

    private final UuidIdentifierGenerator generator =
            new UuidIdentifierGenerator();

    @Test
    void shouldGenerateUuid() {
        UUID identifier = generator.generate();

        assertNotNull(identifier);
    }

    @Test
    void shouldGenerateDifferentIdentifiers() {
        UUID firstIdentifier = generator.generate();
        UUID secondIdentifier = generator.generate();

        assertNotEquals(firstIdentifier, secondIdentifier);
    }
}