package com.sixpay.payment.infrastructure.idempotency;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes a deterministic SHA-256 fingerprint from an already canonicalized
 * Payment request representation.
 *
 * <p>Canonicalization belongs to the future application boundary. This
 * component deliberately hashes the supplied representation without silently
 * reordering or dropping fields.</p>
 */
@Component
public final class PaymentIdempotencyHasher {

    public String hash(String canonicalRequest) {
        if (canonicalRequest == null
                || canonicalRequest.isBlank()) {
            throw new IllegalArgumentException(
                    "Canonical Payment request must not be blank"
            );
        }

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            canonicalRequest.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return toLowercaseHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private static String toLowercaseHex(byte[] bytes) {
        StringBuilder value =
                new StringBuilder(bytes.length * 2);

        for (byte current : bytes) {
            value.append(
                    Character.forDigit(
                            (current >>> 4) & 0x0f,
                            16
                    )
            );
            value.append(
                    Character.forDigit(
                            current & 0x0f,
                            16
                    )
            );
        }

        return value.toString();
    }
}
