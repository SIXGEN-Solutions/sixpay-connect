package com.sixpay.bootstrap.configuration;

import com.sixpay.payment.infrastructure.idempotency.PaymentOtpIdempotencyFingerprintSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PaymentOtpLocalIdempotencyConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PaymentOtpLocalIdempotencyConfiguration.class);

    private final PaymentOtpLocalIdempotencyConfiguration config =
            new PaymentOtpLocalIdempotencyConfiguration();

    private static String encoded(String material) {
        return Base64.getEncoder().encodeToString(
                material.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void disabledByDefaultWithoutAnySpringProfile() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertFalse(context.containsBean("paymentOtpIdempotencyFingerprintSet"));
        });
    }

    @Test
    void enabledWithSecretBuildsBeanWithoutAnySpringProfile() {
        runner.withPropertyValues(
                "SIXPAY_PAYMENT_OTP_HMAC_ENABLED=true",
                "SIXPAY_PAYMENT_OTP_HMAC_KEY_BASE64=" + encoded("current-local-test-key-material-32bytes-2026")
        ).run(context -> {
            assertNull(context.getStartupFailure());
            assertNotNull(context.getBean(PaymentOtpIdempotencyFingerprintSet.class));
        });
    }

    @Test
    void enabledWithoutSecretFailsAtStartup() {
        runner.withPropertyValues("SIXPAY_PAYMENT_OTP_HMAC_ENABLED=true")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void enabledWithWeakSecretFailsAtStartup() {
        runner.withPropertyValues(
                "SIXPAY_PAYMENT_OTP_HMAC_ENABLED=true",
                "SIXPAY_PAYMENT_OTP_HMAC_KEY_BASE64=" + encoded("short")
        ).run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void localKeyBuildsHmacAndSupportsPreviousKeyForReplay() {
        PaymentOtpIdempotencyFingerprintSet current = config.paymentOtpIdempotencyFingerprintSet(
                encoded("current-local-test-key-material-32bytes-2026"), "");
        PaymentOtpIdempotencyFingerprintSet rotated = config.paymentOtpIdempotencyFingerprintSet(
                encoded("next-local-test-key-material-32bytes-2026"),
                encoded("current-local-test-key-material-32bytes-2026"));
        var old = current.candidates("PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV", "123456".toCharArray());
        var afterRotation = rotated.candidates("PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV", "123456".toCharArray());
        assertEquals(1, old.size());
        assertEquals(2, afterRotation.size());
        assertEquals(old.get(0), afterRotation.get(1));
        assertNotEquals(old.get(0), afterRotation.get(0));
    }

    @Test
    void invalidAndWeakKeysAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> config.paymentOtpIdempotencyFingerprintSet("not base64!", ""));
        assertThrows(IllegalArgumentException.class,
                () -> config.paymentOtpIdempotencyFingerprintSet(encoded("short"), ""));
        assertThrows(IllegalArgumentException.class,
                () -> config.paymentOtpIdempotencyFingerprintSet("", ""));
        assertThrows(IllegalArgumentException.class,
                () -> config.paymentOtpIdempotencyFingerprintSet(
                        encoded("current-local-test-key-material-32bytes-2026"), "bad!"));
    }
}
