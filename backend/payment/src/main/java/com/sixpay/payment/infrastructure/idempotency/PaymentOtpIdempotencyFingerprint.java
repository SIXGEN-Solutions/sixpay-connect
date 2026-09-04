package com.sixpay.payment.infrastructure.idempotency;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Produces the secret-safe idempotency fingerprint for the public
 * Payment-confirmation verify request.
 *
 * <p>The logical request defined by the current public contract consists of
 * the Payment reference carried by the path and the transient OTP carried by
 * the request body. The OTP is never persisted: only the keyed HMAC result is
 * eligible for use as {@code payment_idempotency.request_hash}.</p>
 *
 * <p>The HMAC key is a dedicated Security secret and must be supplied by the
 * runtime secret-management boundary (Vault or an equivalent approved secret
 * manager). This class deliberately does not define a property name, Vault
 * path, default key, rotation policy or runtime wiring.</p>
 *
 * <p>This component is intentionally separate from
 * {@link PaymentIdempotencyHasher}: ordinary Payment requests may use the
 * existing unkeyed SHA-256 fingerprint, while OTP-bearing requests require a
 * keyed construction because the OTP has low entropy.</p>
 */
public final class PaymentOtpIdempotencyFingerprint {

    static final String ALGORITHM = "HmacSHA256";

    private static final byte[] DOMAIN_SEPARATOR =
            "SIXPAY_PAYMENT_CONFIRMATION_VERIFY_V1"
                    .getBytes(StandardCharsets.US_ASCII);

    private final SecretKey key;

    public PaymentOtpIdempotencyFingerprint(SecretKey key) {
        this.key = Objects.requireNonNull(
                key,
                "OTP idempotency HMAC key"
        );
    }

    /**
     * Computes a deterministic lowercase hexadecimal HMAC without building a
     * canonical String containing the OTP.
     *
     * @param paymentReference public SIXPAY Payment reference from the request
     *                         path
     * @param otp transient OTP received for verification
     * @return 64-character lowercase HMAC-SHA-256 fingerprint
     */
    public String fingerprint(
            String paymentReference,
            String otp
    ) {
        requireText(
                paymentReference,
                "Payment reference"
        );
        requireText(
                otp,
                "OTP"
        );

        byte[] paymentReferenceBytes =
                paymentReference.getBytes(StandardCharsets.UTF_8);
        byte[] otpBytes = otp.getBytes(StandardCharsets.UTF_8);

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);

            updateLengthPrefixed(mac, DOMAIN_SEPARATOR);
            updateLengthPrefixed(mac, paymentReferenceBytes);
            updateLengthPrefixed(mac, otpBytes);

            return toLowercaseHex(mac.doFinal());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "HMAC-SHA-256 OTP fingerprint is unavailable",
                    exception
            );
        } finally {
            /*
             * The inbound String is owned by the HTTP/request layer and cannot
             * be zeroed by this component. Any byte[] copy created here is
             * cleared immediately after use.
             */
            Arrays.fill(otpBytes, (byte) 0);
        }
    }

    private static void updateLengthPrefixed(
            Mac mac,
            byte[] value
    ) {
        mac.update(
                ByteBuffer.allocate(Integer.BYTES)
                        .putInt(value.length)
                        .array()
        );
        mac.update(value);
    }

    private static void requireText(
            String value,
            String label
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
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
