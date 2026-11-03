package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentInitiationReplayCodecTest {

    @Test
    void roundTripsAcceptedResultWithoutSensitiveData() {
        PaymentInitiationReplayCodec codec =
                new PaymentInitiationReplayCodec();

        InitiateDebitResult original =
                InitiateDebitResult.awaitingOtp(
                        new PaymentId(UUID.randomUUID()),
                        PublicPaymentReference.of(
                                "PAY-1234567890ABCDEFGHJKMNPQRS"
                        ),
                        "AVI-2025-00045678",
                        Money.of(
                                new BigDecimal("600000"),
                                "XAF"
                        ),
                        Instant.parse(
                                "2026-08-03T10:30:00Z"
                        ),
                        new com.sixpay.payment.application.view.PaymentConfirmationView(
                                PublicPaymentReference.of("PAY-1234567890ABCDEFGHJKMNPQRS"),
                                com.sixpay.payment.domain.model.ConfirmationChallengeStatus.ACTIVE,
                                com.sixpay.payment.domain.model.ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                                null,
                                Instant.parse("2026-08-03T10:30:01Z"),
                                Instant.parse("2026-08-03T10:35:01Z"),
                                null,
                                false
                        )
                );

        String encoded = codec.encode(original);
        InitiateDebitResult decoded =
                codec.decode(encoded);

        assertThat(decoded).isEqualTo(original);
        assertThat(encoded)
                .doesNotContain("10005-")
                .doesNotContain("TRESOR_PAY");
    }

    @Test
    void jsonbPayloadIsObjectAndSupportsDurableReplay() throws Exception {
        PaymentInitiationReplayCodec codec = new PaymentInitiationReplayCodec();
        PublicPaymentReference reference = PublicPaymentReference.of(
                "PAY-1234567890ABCDEFGHJKMNPQRS");
        InitiateDebitResult result = InitiateDebitResult.awaitingOtp(
                new PaymentId(UUID.fromString("8b3f1944-400f-4d05-ad1a-80f6b68ed92c")),
                reference,
                "AVI-2025-00045678",
                Money.of(new BigDecimal("600000.00"), "XAF"),
                Instant.parse("2026-08-03T10:30:00Z"),
                new com.sixpay.payment.application.view.PaymentConfirmationView(
                        reference,
                        com.sixpay.payment.domain.model.ConfirmationChallengeStatus.ACTIVE,
                        com.sixpay.payment.domain.model.ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                        null,
                        Instant.parse("2026-08-03T10:30:01Z"),
                        Instant.parse("2026-08-03T10:35:01Z"),
                        null,
                        false
                )
        );
        String encoded = codec.encode(result);
        var json = new tools.jackson.databind.ObjectMapper().readTree(encoded);
        assertThat(json.isObject()).isTrue();
        assertThat(json.get("version").asText()).isEqualTo("v2");
        assertThat(json.get("paymentReference").asText()).isEqualTo(reference.value());
        assertThat(json.get("challengeStatus").asText()).isEqualTo("ACTIVE");
        // AWAITING_OTP is an approved status, not a persisted OTP secret.
        assertThat(encoded).doesNotContain("v2|", "TRESOR_PAY");
        assertThat(json.has("otp")).isFalse();
        assertThat(codec.decode(encoded)).isEqualTo(result);
    }

    @Test
    void decodesHistoricalV2PipePayloadWithoutSchemaChange() {
        String historical = String.join("|",
                "v2",
                "8b3f1944-400f-4d05-ad1a-80f6b68ed92c",
                "PAY-1234567890ABCDEFGHJKMNPQRS",
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                        "AVI-2025-00045678".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "600000",
                "XAF",
                "2026-08-03T10:30:00Z",
                "AWAITING_OTP",
                "ACTIVE",
                "CHALLENGE_ACTIVE",
                "~",
                "2026-08-03T10:30:01Z",
                "2026-08-03T10:35:01Z",
                "~",
                "false"
        );
        InitiateDebitResult decoded = new PaymentInitiationReplayCodec().decode(historical);
        assertThat(decoded.paymentReference().value())
                .isEqualTo("PAY-1234567890ABCDEFGHJKMNPQRS");
        assertThat(decoded.endToEndId()).isEqualTo("AVI-2025-00045678");
        assertThat(decoded.confirmationChallenge().status().name()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsUnknownJsonReplayVersion() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new PaymentInitiationReplayCodec().decode("{\"version\":\"v3\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Payment initiation replay payload");
    }

}
