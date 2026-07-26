package com.sixpay.integration.http;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRestClientFactoryTest {

    @Test
    void shouldCreateRestClient() {
        RestClientFactory factory =
                new DefaultRestClientFactory(
                        RestClient.builder()
                );

        HttpClientSettings settings =
                new HttpClientSettings(
                        URI.create(
                                "https://amplitude.example.com"
                        ),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5)
                );

        RestClient restClient =
                factory.create(settings);

        assertNotNull(restClient);
    }

    @Test
    void shouldRejectNullSettings() {
        RestClientFactory factory =
                new DefaultRestClientFactory(
                        RestClient.builder()
                );

        assertThrows(
                NullPointerException.class,
                () -> factory.create(null)
        );
    }
}