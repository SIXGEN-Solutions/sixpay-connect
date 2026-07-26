package com.sixpay.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemTimeProviderTest {

    @Test
    void shouldReturnInstantProvidedByClock() {
        Instant expectedInstant =
                Instant.parse("2026-07-26T12:00:00Z");

        Clock fixedClock = Clock.fixed(
                expectedInstant,
                ZoneOffset.UTC
        );

        TimeProvider timeProvider =
                new SystemTimeProvider(fixedClock);

        assertEquals(expectedInstant, timeProvider.now());
    }

    @Test
    void shouldRejectNullClock() {
        assertThrows(
                NullPointerException.class,
                () -> new SystemTimeProvider(null)
        );
    }
}