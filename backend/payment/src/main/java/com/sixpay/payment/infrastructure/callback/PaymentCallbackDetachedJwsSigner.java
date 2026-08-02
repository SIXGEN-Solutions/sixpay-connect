package com.sixpay.payment.infrastructure.callback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.payment.callback",
        name = "enabled",
        havingValue = "true"
)
public final class PaymentCallbackDetachedJwsSigner {

    private final ObjectMapper objectMapper;
    private final PaymentCallbackProperties properties;

    public PaymentCallbackDetachedJwsSigner(
            ObjectMapper objectMapper,
            PaymentCallbackProperties properties
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);
        properties.validateEnabledConfiguration();
    }

    public String sign(byte[] payload) {
        Objects.requireNonNull(payload, "Callback payload");

        try {
            String protectedHeader = base64Url(
                    objectMapper.writeValueAsBytes(
                            Map.of(
                                    "alg", "RS256",
                                    "kid", properties
                                            .getSigningKeyId(),
                                    "typ", "JOSE"
                            )
                    )
            );
            String encodedPayload = base64Url(payload);
            byte[] signingInput = (
                    protectedHeader
                            + "."
                            + encodedPayload
            ).getBytes(StandardCharsets.US_ASCII);

            Signature signature =
                    Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey());
            signature.update(signingInput);

            return protectedHeader
                    + ".."
                    + base64Url(signature.sign());
        } catch (Exception exception) {
            throw new PaymentCallbackSigningException(
                    "Cannot sign Payment callback",
                    exception
            );
        }
    }

    private PrivateKey privateKey() throws Exception {
        String normalized = properties
                .getSigningPrivateKeyPem()
                .replace(
                        "-----BEGIN PRIVATE KEY-----",
                        ""
                )
                .replace(
                        "-----END PRIVATE KEY-----",
                        ""
                )
                .replaceAll("\\s", "");

        byte[] encoded =
                Base64.getDecoder().decode(normalized);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(
                        new PKCS8EncodedKeySpec(encoded)
                );
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }
}
