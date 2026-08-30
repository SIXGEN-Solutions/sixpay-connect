package com.sixpay.payment.infrastructure.idempotency;

import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOtpIdempotencyFingerprintSetTest {

    @Test
    void keepsCurrentFingerprintFirstAndPreviousFingerprintsForReplay() {
        PaymentOtpIdempotencyFingerprint current =
                fingerprint("current-key-material-for-lot-1.4");
        PaymentOtpIdempotencyFingerprint previous =
                fingerprint("previous-key-material-for-lot-1.4");

        PaymentOtpIdempotencyFingerprintSet set =
                new PaymentOtpIdempotencyFingerprintSet(
                        List.of(current, previous)
                );

        List<String> candidates =
                set.candidates(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                        "123456".toCharArray()
                );

        assertThat(candidates).hasSize(2);
        assertThat(candidates.get(0))
                .isEqualTo(
                        current.fingerprint(
                                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                "123456"
                        )
                );
    }

    private static PaymentOtpIdempotencyFingerprint fingerprint(
            String key
    ) {
        return new PaymentOtpIdempotencyFingerprint(
                new SecretKeySpec(
                        key.getBytes(StandardCharsets.UTF_8),
                        PaymentOtpIdempotencyFingerprint.ALGORITHM
                )
        );
    }
}
