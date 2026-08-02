package com.sixpay.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentInitiationContextTest {

    @Test
    void acceptsCompleteNonSecretTresorPayContext() {
        PaymentInitiationContext context =
                new PaymentInitiationContext(
                        "TRESOR_PAY",
                        "TP_APP_001",
                        "Société ABC SARL",
                        ClaimType.AVI,
                        "100200300",
                        Instant.parse("2026-08-03T10:30:00Z"),
                        CallbackEndpoint.of(
                                "https://tresorpay.cm/v1/callbacks/payment-status"
                        )
                );

        assertThat(context.partnerLoginName())
                .isEqualTo("TRESOR_PAY");
        assertThat(context.optionalApplicationId())
                .contains("TP_APP_001");
        assertThat(context.claimType())
                .isEqualTo(ClaimType.AVI);
    }

    @Test
    void applicationIdIsOptional() {
        PaymentInitiationContext context =
                new PaymentInitiationContext(
                        "TRESOR_PAY",
                        null,
                        "Société ABC SARL",
                        ClaimType.IM7,
                        "100200300",
                        Instant.parse("2026-08-03T10:30:00Z"),
                        CallbackEndpoint.of(
                                "https://tresorpay.cm/callback"
                        )
                );

        assertThat(context.optionalApplicationId()).isEmpty();
    }

    @Test
    void callbackMustUseHttps() {
        assertThatThrownBy(() ->
                CallbackEndpoint.of(
                        "http://tresorpay.cm/callback"
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
