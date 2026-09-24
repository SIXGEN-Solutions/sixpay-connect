package com.sixpay.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentInitiationContextTest {

    @Test
    void acceptsCompleteNonSecretPartnerContext() {
        PaymentInitiationContext context =
                new PaymentInitiationContext(
                        CanonicalPartnerIdentity.from(
                                "11111111-2222-3333-4444-555555555555"
                        ),
                        "TP_APP_001",
                        "Société ABC SARL",
                        ClaimType.AVI,
                        "100200300",
                        Instant.parse("2026-08-03T10:30:00Z"),
                        CallbackEndpoint.of(
                                "https://partner.cm/v1/callbacks/payment-status"
                        )
                );

        assertThat(context.partnerIdentity().toString())
                .isEqualTo(
                        "11111111-2222-3333-4444-555555555555"
                );
        assertThat(context.optionalApplicationId())
                .contains("TP_APP_001");
        assertThat(context.claimType())
                .isEqualTo(ClaimType.AVI);
        assertThat(context.treasuryPaymentContext().claimType())
                .isEqualTo(ClaimType.AVI);
        assertThat(context.treasuryPaymentContext().taxpayerIdentifier())
                .isEqualTo("100200300");
    }

    @Test
    void applicationIdIsOptional() {
        PaymentInitiationContext context =
                new PaymentInitiationContext(
                        CanonicalPartnerIdentity.from(
                                "11111111-2222-3333-4444-555555555555"
                        ),
                        null,
                        "Société ABC SARL",
                        ClaimType.IM7,
                        "100200300",
                        Instant.parse("2026-08-03T10:30:00Z"),
                        CallbackEndpoint.of(
                                "https://partner.cm/callback"
                        )
                );

        assertThat(context.optionalApplicationId()).isEmpty();
    }

    @Test
    void callbackMustUseHttps() {
        assertThatThrownBy(() ->
                CallbackEndpoint.of(
                        "http://partner.cm/callback"
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
