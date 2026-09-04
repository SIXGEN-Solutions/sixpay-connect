package com.sixpay.payment.infrastructure.idempotency;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class PaymentOtpIdempotencyFingerprintSet {

    private final List<PaymentOtpIdempotencyFingerprint> fingerprints;

    public PaymentOtpIdempotencyFingerprintSet(
            List<PaymentOtpIdempotencyFingerprint> fingerprints
    ) {
        if (fingerprints == null || fingerprints.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one OTP idempotency fingerprint is required"
            );
        }

        this.fingerprints = List.copyOf(
                fingerprints.stream()
                        .map(value -> Objects.requireNonNull(
                                value,
                                "OTP idempotency fingerprint"
                        ))
                        .toList()
        );
    }

    public List<String> candidates(
            String paymentReference,
            char[] otp
    ) {
        Objects.requireNonNull(otp, "OTP");
        if (otp.length == 0) {
            throw new IllegalArgumentException("OTP must not be empty");
        }

        String transientOtp = new String(otp);
        try {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (PaymentOtpIdempotencyFingerprint fingerprint
                    : fingerprints) {
                values.add(
                        fingerprint.fingerprint(
                                paymentReference,
                                transientOtp
                        )
                );
            }
            return List.copyOf(values);
        } finally {
            transientOtp = null;
        }
    }
}
