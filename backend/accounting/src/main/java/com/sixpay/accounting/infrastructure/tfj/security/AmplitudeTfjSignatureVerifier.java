package com.sixpay.accounting.infrastructure.tfj.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public final class AmplitudeTfjSignatureVerifier {

    public static final String CALLBACK_TARGET =
            "/webhooks/v1/amplitude/end-of-day-confirmations";

    private static final Duration ALLOWED_CLOCK_SKEW =
            Duration.ofMinutes(5);

    private final AmplitudeTfjSignatureProperties properties;
    private final Clock clock;

    public AmplitudeTfjSignatureVerifier(
            AmplitudeTfjSignatureProperties properties,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
    }

    public void verify(
            byte[] rawBody,
            String signature,
            String keyId,
            String timestamp
    ) {
        if (!properties.enabled()) {
            throw new SecurityException(
                    "TFJ webhook signature verification is disabled"
            );
        }

        if (signature == null || !signature.startsWith("v1=")) {
            throw new SecurityException(
                    "Invalid Amplitude signature format"
            );
        }
        if (keyId == null || keyId.isBlank()) {
            throw new SecurityException(
                    "Missing Amplitude signature key id"
            );
        }
        if (timestamp == null || !timestamp.matches("^[0-9]{10}$")) {
            throw new SecurityException(
                    "Invalid Amplitude signature timestamp"
            );
        }

        Instant signedAt = Instant.ofEpochSecond(
                Long.parseLong(timestamp)
        );
        Instant now = clock.instant();
        if (signedAt.isBefore(now.minus(ALLOWED_CLOCK_SKEW))
                || signedAt.isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
            throw new SecurityException(
                    "Amplitude signature timestamp is outside the five-minute window"
            );
        }

        String encodedSecret =
                properties.signatureKeysBase64().get(keyId);
        if (encodedSecret == null) {
            throw new SecurityException(
                    "Unknown Amplitude signature key id"
            );
        }

        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Configured Amplitude signature key is not valid Base64",
                    exception
            );
        }

        String bodyDigest =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(sha256(rawBody));

        String signedPayload =
                timestamp
                        + ".POST."
                        + CALLBACK_TARGET
                        + "."
                        + bodyDigest;

        String expected =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                hmacSha256(
                                        secret,
                                        signedPayload.getBytes(
                                                StandardCharsets.UTF_8
                                        )
                                )
                        );

        String actual = signature.substring(3);

        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new SecurityException(
                    "Invalid Amplitude signature"
            );
        }
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    exception
            );
        }
    }

    private static byte[] hmacSha256(
            byte[] key,
            byte[] value
    ) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "HmacSHA256 unavailable",
                    exception
            );
        }
    }
}
