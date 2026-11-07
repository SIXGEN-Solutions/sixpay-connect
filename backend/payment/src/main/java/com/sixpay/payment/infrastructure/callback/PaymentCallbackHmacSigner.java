package com.sixpay.payment.infrastructure.callback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "sixpay.payment.callback", name = "enabled", havingValue = "true")
public final class PaymentCallbackHmacSigner {
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private final PaymentCallbackProperties properties;

    public PaymentCallbackHmacSigner(PaymentCallbackProperties properties) {
        this.properties = Objects.requireNonNull(properties);
        properties.validateEnabledConfiguration();
    }

    public SignatureHeaders sign(String httpMethod, String requestTarget, byte[] payload, Instant timestamp) {
        Objects.requireNonNull(payload, "Callback payload");
        Objects.requireNonNull(timestamp, "Signature timestamp");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            String signingInput = timestamp.getEpochSecond() + "." + httpMethod.toUpperCase() + "." + requestTarget + "." + BASE64_URL.encodeToString(digest);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getSigningSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return new SignatureHeaders(
                    "v1=" + BASE64_URL.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8))),
                    properties.getSigningKeyId(),
                    Long.toString(timestamp.getEpochSecond())
            );
        } catch (Exception exception) {
            throw new PaymentCallbackSigningException("Cannot sign Payment callback", exception);
        }
    }

    public record SignatureHeaders(String signature, String keyId, String timestamp) {}
}
