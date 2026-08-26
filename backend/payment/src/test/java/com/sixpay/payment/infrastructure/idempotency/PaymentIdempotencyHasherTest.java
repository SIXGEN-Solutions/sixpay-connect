package com.sixpay.payment.infrastructure.idempotency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentIdempotencyHasherTest {

    private final PaymentIdempotencyHasher hasher =
            new PaymentIdempotencyHasher();

    @Test
    void producesStableLowercaseSha256() {
        String first = hasher.hash(
                "{\"amount\":\"1000.00\",\"currency\":\"XAF\"}"
        );
        String second = hasher.hash(
                "{\"amount\":\"1000.00\",\"currency\":\"XAF\"}"
        );

        assertThat(first)
                .isEqualTo(second)
                .matches("^[0-9a-f]{64}$");
    }

    @Test
    void differentCanonicalRequestsProduceDifferentHashes() {
        String first = hasher.hash(
                "{\"amount\":\"1000.00\",\"currency\":\"XAF\"}"
        );
        String second = hasher.hash(
                "{\"amount\":\"2000.00\",\"currency\":\"XAF\"}"
        );

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsBlankCanonicalRequest() {
        assertThatThrownBy(() -> hasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
