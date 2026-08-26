package com.sixpay.customer.observation.configuration;

import org.springframework.boot.context.properties
        .ConfigurationProperties;

import java.util.Base64;
import java.util.Objects;

/**
 * Runtime configuration for the internal Observed Customer read model.
 */
@ConfigurationProperties(
        prefix = "sixpay.customer.observation.query"
)
public record ObservedCustomerQueryProperties(
        boolean enabled,
        String cursorKeyBase64
) {

    private static final int MINIMUM_KEY_BYTES = 32;

    public ObservedCustomerQueryProperties {
        if (enabled) {
            cursorKeyBase64 = Objects.requireNonNull(
                    cursorKeyBase64,
                    "cursorKeyBase64 is required when "
                            + "Observed Customer query is enabled"
            ).strip();

            byte[] decoded;

            try {
                decoded = Base64.getDecoder().decode(
                        cursorKeyBase64
                );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "cursorKeyBase64 must be valid Base64",
                        exception
                );
            }

            if (decoded.length < MINIMUM_KEY_BYTES) {
                throw new IllegalArgumentException(
                        "cursorKeyBase64 must encode at least "
                                + MINIMUM_KEY_BYTES
                                + " bytes"
                );
            }
        }
    }

    public byte[] decodedCursorKey() {
        if (!enabled) {
            throw new IllegalStateException(
                    "Observed Customer query is disabled"
            );
        }

        return Base64.getDecoder().decode(
                cursorKeyBase64
        );
    }
}
