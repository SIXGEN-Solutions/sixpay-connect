package com.sixpay.integration.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalSystemTest {

    @Test
    void shouldCreateExternalSystem() {
        ExternalSystem system =
                ExternalSystem.of("EXTERNAL_REPORTING");

        assertEquals(
                "EXTERNAL_REPORTING",
                system.name()
        );

        assertEquals(
                "EXTERNAL_REPORTING",
                system.toString()
        );
    }

    @Test
    void shouldExposeKnownSystems() {
        assertEquals(
                "AMPLITUDE",
                ExternalSystem.AMPLITUDE.name()
        );

        assertEquals(
                "TRESORPAY",
                ExternalSystem.TRESORPAY.name()
        );
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ExternalSystem.of(" ")
        );
    }
}