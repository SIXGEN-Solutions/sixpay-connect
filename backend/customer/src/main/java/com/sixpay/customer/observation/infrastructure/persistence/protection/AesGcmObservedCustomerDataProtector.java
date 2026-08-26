package com.sixpay.customer.observation.infrastructure.persistence.protection;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * AES-256-GCM protection with a deterministic HMAC-SHA-256 lookup hash.
 */
public final class AesGcmObservedCustomerDataProtector
        implements ObservedCustomerDataProtector {

    private static final String VALUE_PREFIX = "v1:";
    private static final String AES_TRANSFORMATION =
            "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKey encryptionKey;
    private final SecretKey lookupKey;
    private final SecureRandom secureRandom;

    public AesGcmObservedCustomerDataProtector(
            String masterKeyBase64
    ) {
        this(masterKeyBase64, new SecureRandom());
    }

    AesGcmObservedCustomerDataProtector(
            String masterKeyBase64,
            SecureRandom secureRandom
    ) {
        Objects.requireNonNull(
                masterKeyBase64,
                "masterKeyBase64 is required"
        );
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom is required"
        );

        byte[] masterKey;
        try {
            masterKey = Base64.getDecoder()
                    .decode(masterKeyBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "masterKeyBase64 is not valid Base64",
                    exception
            );
        }

        if (masterKey.length < 32) {
            throw new IllegalArgumentException(
                    "master key must contain at least 32 bytes"
            );
        }

        this.encryptionKey = new SecretKeySpec(
                derive(masterKey, "observed-customer-encryption"),
                "AES"
        );
        this.lookupKey = new SecretKeySpec(
                derive(masterKey, "observed-customer-lookup"),
                "HmacSHA256"
        );
    }

    @Override
    public String protect(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(
                    AES_TRANSFORMATION
            );
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    encryptionKey,
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            byte[] encrypted = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            return VALUE_PREFIX
                    + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(iv)
                    + ":"
                    + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to protect Observed Customer value",
                    exception
            );
        }
    }

    @Override
    public String reveal(String protectedValue) {
        if (protectedValue == null) {
            return null;
        }
        if (!protectedValue.startsWith(VALUE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Unsupported protected-value version"
            );
        }

        String[] parts = protectedValue.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Malformed protected value"
            );
        }

        try {
            byte[] iv = Base64.getUrlDecoder()
                    .decode(parts[1]);
            byte[] encrypted = Base64.getUrlDecoder()
                    .decode(parts[2]);

            Cipher cipher = Cipher.getInstance(
                    AES_TRANSFORMATION
            );
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    encryptionKey,
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8
            );
        } catch (GeneralSecurityException
                 | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unable to reveal Observed Customer value",
                    exception
            );
        }
    }

    @Override
    public String searchHash(String normalizedValue) {
        Objects.requireNonNull(
                normalizedValue,
                "normalizedValue is required"
        );

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(lookupKey);
            return HexFormat.of().formatHex(
                    mac.doFinal(
                            normalizedValue.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to derive Observed Customer search hash",
                    exception
            );
        }
    }

    private static byte[] derive(
            byte[] masterKey,
            String purpose
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            digest.update(masterKey);
            digest.update(
                    purpose.getBytes(StandardCharsets.UTF_8)
            );
            return digest.digest();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to derive protection key",
                    exception
            );
        }
    }
}
