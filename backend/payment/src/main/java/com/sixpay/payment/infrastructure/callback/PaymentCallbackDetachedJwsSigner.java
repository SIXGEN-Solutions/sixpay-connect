package com.sixpay.payment.infrastructure.callback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
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
    private final CallbackSigningKeyProvider keyProvider;

    public PaymentCallbackDetachedJwsSigner(
            ObjectMapper objectMapper,
            CallbackSigningKeyProvider keyProvider
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.keyProvider = Objects.requireNonNull(keyProvider);
    }

    public String sign(byte[] payload) {
        Objects.requireNonNull(payload, "Callback payload");

        try {
            CallbackSigningKeyProvider.SigningKey key =
                    keyProvider.current();

            String protectedHeader = base64Url(
                    objectMapper.writeValueAsBytes(
                            Map.of(
                                    "alg", key.algorithm(),
                                    "kid", key.keyId(),
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
            signature.initSign(key.privateKey());
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

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }
}
