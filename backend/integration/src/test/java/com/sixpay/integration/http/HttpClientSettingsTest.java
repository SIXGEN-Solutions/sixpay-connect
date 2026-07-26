package com.sixpay.integration.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpClientSettingsTest {

    @Test
    void shouldCreateHttpClientSettings() {
        HttpClientSettings settings =
                new HttpClientSettings(
                        URI.create(
                                "https://amplitude.example.com"
                        ),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                );

        assertEquals(
                URI.create(
                        "https://amplitude.example.com"
                ),
                settings.baseUri()
        );

        assertEquals(
                Duration.ofSeconds(2),
                settings.connectTimeout()
        );

        assertEquals(
                Duration.ofSeconds(5),
                settings.readTimeout()
        );
    }

    @Test
    void shouldRejectRelativeUri() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpClientSettings(
                        URI.create("/amplitude"),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                )
        );
    }

    @Test
    void shouldRejectUnsupportedScheme() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpClientSettings(
                        URI.create(
                                "ftp://amplitude.example.com"
                        ),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                )
        );
    }

    @Test
    void shouldRejectZeroTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpClientSettings(
                        URI.create(
                                "https://amplitude.example.com"
                        ),
                        Duration.ZERO,
                        Duration.ofSeconds(5)
                )
        );
    }
}