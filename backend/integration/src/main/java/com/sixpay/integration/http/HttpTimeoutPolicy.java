package com.sixpay.integration.http;

import java.time.Duration;
import java.util.Objects;

public record HttpTimeoutPolicy(Duration connectTimeout, Duration readTimeout) {
    public static final HttpTimeoutPolicy DEFAULT =
            new HttpTimeoutPolicy(Duration.ofSeconds(2), Duration.ofSeconds(5));
    public HttpTimeoutPolicy {
        connectTimeout = positive(connectTimeout, "connectTimeout");
        readTimeout = positive(readTimeout, "readTimeout");
        if (readTimeout.compareTo(connectTimeout) < 0) {
            throw new IllegalArgumentException("readTimeout must be >= connectTimeout");
        }
    }
    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
}
