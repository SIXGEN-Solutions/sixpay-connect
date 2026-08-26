package com.sixpay.integration.http;

import com.sixpay.common.validation.Preconditions;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

/**
 * Defines the technical configuration of an external HTTP client.
 *
 * @param baseUri external service base URI
 * @param connectTimeout connection timeout
 * @param readTimeout response timeout
 */
public record HttpClientSettings(
        URI baseUri,
        Duration connectTimeout,
        Duration readTimeout
) {

    private static final Set<String> SUPPORTED_SCHEMES =
            Set.of("http", "https");

    public HttpClientSettings {
        baseUri = validateBaseUri(baseUri);

        connectTimeout = requirePositiveDuration(
                connectTimeout,
                "Connect timeout must be positive"
        );

        readTimeout = requirePositiveDuration(
                readTimeout,
                "Read timeout must be positive"
        );
    }

    private static URI validateBaseUri(URI baseUri) {
        URI validatedUri = Preconditions.requireNonNull(
                baseUri,
                "Base URI must not be null"
        );

        if (!validatedUri.isAbsolute()) {
            throw new IllegalArgumentException(
                    "Base URI must be absolute"
            );
        }

        if (!SUPPORTED_SCHEMES.contains(
                validatedUri.getScheme().toLowerCase()
        )) {
            throw new IllegalArgumentException(
                    "Base URI must use HTTP or HTTPS"
            );
        }

        return validatedUri;
    }

    private static Duration requirePositiveDuration(
            Duration duration,
            String message
    ) {
        Duration validatedDuration =
                Preconditions.requireNonNull(
                        duration,
                        message
                );

        if (validatedDuration.isZero()
                || validatedDuration.isNegative()) {
            throw new IllegalArgumentException(message);
        }

        return validatedDuration;
    }
}