package com.sixpay.payment.infrastructure.callback;

import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

public final class PemCallbackSigningKeyProvider
        implements CallbackSigningKeyProvider {

    private final PaymentCallbackProperties properties;

    public PemCallbackSigningKeyProvider(
            PaymentCallbackProperties properties
    ) {
        this.properties = Objects.requireNonNull(properties);
        properties.validateEnabledConfiguration();
    }

    @Override
    public SigningKey current() {
        try {
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

            return new SigningKey(
                    properties.getSigningKeyId(),
                    "RS256",
                    KeyFactory.getInstance("RSA")
                            .generatePrivate(
                                    new PKCS8EncodedKeySpec(encoded)
                            )
            );
        } catch (Exception exception) {
            throw new PaymentCallbackSigningException(
                    "Cannot load Payment callback signing key",
                    exception
            );
        }
    }
}
