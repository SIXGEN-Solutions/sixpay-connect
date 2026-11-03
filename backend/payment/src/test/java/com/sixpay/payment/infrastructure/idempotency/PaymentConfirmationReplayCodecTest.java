package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentConfirmationReplayCodecTest {
    private final PaymentConfirmationReplayCodec codec =
            new PaymentConfirmationReplayCodec();

    @Test
    void encodesValidJsonbWithAllAuthoritativeFieldsAndRoundTrips() throws Exception {
        PaymentConfirmationBankResult original = new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference("CHL-001"),
                ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                ConfirmationDeliveryChannel.SMS,
                Instant.parse("2026-11-03T20:40:00Z"),
                Instant.parse("2026-11-03T20:45:00Z"),
                null
        );
        String encoded = codec.encode(original);
        var json = new ObjectMapper().readTree(encoded);
        assertThat(json.get("version").asText()).isEqualTo("v1");
        assertThat(json.get("challengeReference").asText()).isEqualTo("CHL-001");
        assertThat(json.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(encoded).doesNotContain("otp", "OTP", "123456");
        assertThat(codec.decode(encoded)).isEqualTo(original);
    }

    @Test
    void verifiesNoOptionalFieldsAndLegacyReplay() {
        PaymentConfirmationBankResult original = new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference("CHL-002"),
                ConfirmationChallengeStatus.REVOKED,
                ConfirmationBusinessCode.CHALLENGE_REVOKED,
                null, null, null, null
        );
        assertThat(codec.decode(codec.encode(original))).isEqualTo(original);
        assertThat(codec.decode("v1|Q0hMLTAwMg|REVOKED|CHALLENGE_REVOKED|-|-|-|-"))
                .isEqualTo(original);
    }

    @Test
    void rejectsUnknownVersion() {
        assertThatThrownBy(() -> codec.decode("{\"version\":\"v2\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bankSuccessFollowedByPersistenceErrorNeverMarksRequestFailed() {
        PaymentId id = new PaymentId(
                UUID.fromString("9b4a6ec9-08b9-467a-9dc3-8cd2efbeed64"));
        PublicPaymentReference paymentRef = PublicPaymentReference.of(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV");
        IdempotencyKey key = IdempotencyKey.of("create-jsonb-recovery-01");
        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        String hash = new PaymentIdempotencyHasher().hash(
                canonicalizer.create(id, paymentRef));
        var transactions = mock(PaymentConfirmationIdempotencyTransactions.class);
        when(transactions.begin(eq("PAYMENT_CONFIRMATION_CREATE"), eq(key.value()),
                eq(hash), eq(List.of(hash)), any(Instant.class)))
                .thenReturn(new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        PaymentIdempotencyDecision.newRequest(), hash))
                .thenReturn(new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        PaymentIdempotencyDecision.inProgress(), hash));
        doThrow(new IllegalStateException("local JSONB write failed"))
                .doNothing()
                .when(transactions).complete(eq("PAYMENT_CONFIRMATION_CREATE"),
                        eq(key.value()), eq(hash), eq(id.value()),
                        eq("ACTIVE"), anyString(), any(Instant.class));
        TimeProvider clock = () -> Instant.parse("2026-11-03T20:41:00Z");
        var adapter = new PaymentConfirmationIdempotencyAdapter(
                canonicalizer, new PaymentIdempotencyHasher(),
                (PaymentOtpIdempotencyFingerprintSet) null,
                transactions, codec, clock);
        PaymentConfirmationBankResult bankResult = new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference("CHL-EXISTING"),
                ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                null, null, null, null);
        AtomicInteger posts = new AtomicInteger();
        AtomicInteger lookups = new AtomicInteger();
        assertThatThrownBy(() -> adapter.executeCreate(id, paymentRef, key,
                () -> { posts.incrementAndGet(); return bankResult; },
                () -> { lookups.incrementAndGet(); return bankResult; }))
                .hasMessageContaining("local JSONB write failed");
        var recovered = adapter.executeCreate(id, paymentRef, key,
                () -> { posts.incrementAndGet(); return bankResult; },
                () -> { lookups.incrementAndGet(); return bankResult; });
        assertThat(recovered.result()).isEqualTo(bankResult);
        assertThat(posts).hasValue(1);
        assertThat(lookups).hasValue(1);
        verify(transactions, never()).fail(anyString(), anyString(), anyString(),
                anyString(), any(Instant.class));
    }
}
