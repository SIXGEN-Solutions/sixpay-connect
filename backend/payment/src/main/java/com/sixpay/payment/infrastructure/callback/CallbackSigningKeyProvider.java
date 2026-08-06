package com.sixpay.payment.infrastructure.callback;

import java.security.PrivateKey;

@FunctionalInterface
public interface CallbackSigningKeyProvider {

    SigningKey current();

    record SigningKey(
            String keyId,
            String algorithm,
            PrivateKey privateKey
    ) {
        public SigningKey {
            if (keyId == null || keyId.isBlank()) {
                throw new IllegalArgumentException(
                        "Callback signing key ID is required"
                );
            }
            if (algorithm == null || algorithm.isBlank()) {
                throw new IllegalArgumentException(
                        "Callback signing algorithm is required"
                );
            }
            if (privateKey == null) {
                throw new IllegalArgumentException(
                        "Callback private key is required"
                );
            }
            keyId = keyId.strip();
            algorithm = algorithm.strip();
        }
    }
}
