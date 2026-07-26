package com.sixpay.integration.http;

/**
 * Custom HTTP headers used by SIXPAY CONNECT integrations.
 */
public final class IntegrationHttpHeaders {

    public static final String CORRELATION_ID =
            "X-Correlation-ID";

    public static final String REQUEST_ID =
            "X-Request-ID";

    public static final String IDEMPOTENCY_KEY =
            "Idempotency-Key";

    private IntegrationHttpHeaders() {
        throw new IllegalStateException(
                "IntegrationHttpHeaders cannot be instantiated"
        );
    }
}