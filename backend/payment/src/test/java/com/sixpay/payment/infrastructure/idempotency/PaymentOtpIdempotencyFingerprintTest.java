package com.sixpay.payment.infrastructure.idempotency;

import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentOtpIdempotencyFingerprintTest {

    private static final String TEST_KEY =
            "lot-0.4.5-test-only-hmac-key-material";

    private final PaymentOtpIdempotencyFingerprint fingerprint =
            new PaymentOtpIdempotencyFingerprint(
                    new SecretKeySpec(
                            TEST_KEY.getBytes(StandardCharsets.UTF_8),
                            PaymentOtpIdempotencyFingerprint.ALGORITHM
                    )
            );

    @Test
    void returnsSameFingerprintForSameLogicalVerifyRequest() {
        String first = fingerprint.fingerprint(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "123456"
        );

        String replay = fingerprint.fingerprint(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "123456"
        );

        assertThat(replay).isEqualTo(first);
    }

    @Test
    void changesFingerprintWhenOtpChanges() {
        String original = fingerprint.fingerprint(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "123456"
        );

        String differentOtp = fingerprint.fingerprint(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "654321"
        );

        assertThat(differentOtp).isNotEqualTo(original);
    }

    @Test
    void changesFingerprintWhenPaymentReferenceChanges() {
        String original = fingerprint.fingerprint(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "123456"
        );

        String differentPayment = fingerprint.fingerprint(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAW",
                "123456"
        );

        assertThat(differentPayment).isNotEqualTo(original);
    }

    @Test
    void producesOnlyOpaqueSha256SizedHexOutput() {
        String otp = "123456";
        String paymentReference =
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV";

        String value = fingerprint.fingerprint(
                paymentReference,
                otp
        );

        assertThat(value)
                .matches("^[0-9a-f]{64}$")
                .doesNotContain(otp)
                .doesNotContain(paymentReference);
    }

    @Test
    void rejectsMissingLogicalRequestFields() {
        assertThatThrownBy(() ->
                fingerprint.fingerprint(
                        "",
                        "123456"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment reference must not be blank");

        assertThatThrownBy(() ->
                fingerprint.fingerprint(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                        " "
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP must not be blank");
    }
}
