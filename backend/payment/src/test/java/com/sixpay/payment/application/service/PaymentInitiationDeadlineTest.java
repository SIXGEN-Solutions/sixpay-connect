package com.sixpay.payment.application.service;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentInitiationDeadlineTest {
    private static final Instant RECEIVED=Instant.parse("2026-09-16T12:00:00Z");

    @Test void expiresExactlyAtDeadline() {
        PaymentInitiationDeadline deadline=new PaymentInitiationDeadline(Duration.ofSeconds(30));
        assertThat(deadline.expired(RECEIVED,RECEIVED.plusSeconds(29))).isFalse();
        assertThat(deadline.expired(RECEIVED,RECEIVED.plusSeconds(30))).isTrue();
    }

    @Test void rejectsNonPositiveDeadline() {
        assertThatThrownBy(() -> new PaymentInitiationDeadline(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
