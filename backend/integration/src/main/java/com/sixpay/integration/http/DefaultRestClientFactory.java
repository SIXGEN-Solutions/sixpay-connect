package com.sixpay.integration.http;

import com.sixpay.common.validation.Preconditions;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Default REST client factory based on Java HttpClient and
 * Spring RestClient.
 */
public final class DefaultRestClientFactory
        implements RestClientFactory {

    private final RestClient.Builder baseBuilder;

    public DefaultRestClientFactory(
            RestClient.Builder baseBuilder
    ) {
        this.baseBuilder = Preconditions.requireNonNull(
                baseBuilder,
                "RestClient builder must not be null"
        );
    }

    @Override
    public RestClient create(HttpClientSettings settings) {
        HttpClientSettings validatedSettings =
                Preconditions.requireNonNull(
                        settings,
                        "HTTP client settings must not be null"
                );

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(
                        validatedSettings.connectTimeout()
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                validatedSettings.readTimeout()
        );

        return baseBuilder.clone()
                .baseUrl(
                        validatedSettings.baseUri().toString()
                )
                .requestFactory(requestFactory)
                .build();
    }
}