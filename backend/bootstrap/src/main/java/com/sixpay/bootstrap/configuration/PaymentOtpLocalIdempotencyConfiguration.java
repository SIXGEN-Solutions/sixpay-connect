package com.sixpay.bootstrap.configuration;

import com.sixpay.payment.infrastructure.idempotency.PaymentOtpIdempotencyFingerprint;
import com.sixpay.payment.infrastructure.idempotency.PaymentOtpIdempotencyFingerprintSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Explicit local-development composition only. Production keys are provisioned
 * by an approved secret-manager boundary, never by this configuration.
 * No default test key, no OTP storage, and no SHA-256 fallback for VERIFY.
 */
@Configuration(proxyBeanMethods = false)
@Profile("payment-banking-local")
public class PaymentOtpLocalIdempotencyConfiguration {

    @Bean
    @ConditionalOnMissingBean(PaymentOtpIdempotencyFingerprintSet.class)
    @ConditionalOnProperty(name = "SIXPAY_PAYMENT_OTP_HMAC_KEY_BASE64")
    PaymentOtpIdempotencyFingerprintSet paymentOtpIdempotencyFingerprintSet(
            @Value("${SIXPAY_PAYMENT_OTP_HMAC_KEY_BASE64}") String current,
            @Value("${SIXPAY_PAYMENT_OTP_HMAC_PREVIOUS_KEYS_BASE64:}") String previous
    ) {
        List<PaymentOtpIdempotencyFingerprint> fingerprints = new ArrayList<>();
        fingerprints.add(fingerprint(current));
        if (previous != null && !previous.isBlank()) {
            for (String previousKey : previous.split(",", -1)) {
                fingerprints.add(fingerprint(previousKey.trim()));
            }
        }
        return new PaymentOtpIdempotencyFingerprintSet(fingerprints);
    }

    private static PaymentOtpIdempotencyFingerprint fingerprint(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException(
                    "OTP HMAC key must be a non-empty Base64 secret"
            );
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("OTP HMAC key must be Base64", invalid);
        }
        try {
            if (bytes.length < 32) {
                throw new IllegalArgumentException("OTP HMAC key requires at least 32 bytes");
            }
            return new PaymentOtpIdempotencyFingerprint(
                    new SecretKeySpec(bytes, "HmacSHA256")
            );
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
