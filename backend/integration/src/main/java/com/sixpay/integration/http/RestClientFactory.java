package com.sixpay.integration.http;

import org.springframework.web.client.RestClient;

/**
 * Creates configured REST clients for external systems.
 */
@FunctionalInterface
public interface RestClientFactory {

    RestClient create(HttpClientSettings settings);
}