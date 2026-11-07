package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.domain.model.*;
import org.junit.jupiter.api.Test;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentConfirmationResendRecoveryE2ETest {
    @Test
    void uncertainResendRecoversOnceWithoutBlindSecondReplace() {
        PaymentId paymentId = new PaymentId(
                UUID.fromString("77777777-7777-4777-8777-777777777777"));
        PublicPaymentReference paymentReference =
                PublicPaymentReference.of("PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC");
        ConfirmationChallengeReference oldChallenge =
                new ConfirmationChallengeReference("CHALLENGE-OLD");
        IdempotencyKey key = IdempotencyKey.of("resend-key-0001");

        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher = new PaymentIdempotencyHasher();
        String requestHash = hasher.hash(
                canonicalizer.replace(paymentId, paymentReference, oldChallenge));

        PaymentConfirmationIdempotencyTransactions transactions =
                mock(PaymentConfirmationIdempotencyTransactions.class);
        when(transactions.begin(
                eq(PaymentConfirmationIdempotencyAdapter.REPLACE_OPERATION),
                eq(key.value()), eq(requestHash), eq(List.of(requestHash)),
                any(Instant.class)))
                .thenReturn(new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        PaymentIdempotencyDecision.newRequest(), requestHash));

        TimeProvider time = mock(TimeProvider.class);
        when(time.now()).thenReturn(
                Instant.parse("2026-09-16T20:00:00Z"),
                Instant.parse("2026-09-16T20:00:01Z"),
                Instant.parse("2026-09-16T20:00:02Z"));

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        new PaymentOtpIdempotencyFingerprintSet(
                                List.of(
                                        new PaymentOtpIdempotencyFingerprint(
                                                new SecretKeySpec(
                                                        "01234567890123456789012345678901"
                                                                .getBytes(StandardCharsets.UTF_8),
                                                        "HmacSHA256"
                                                )
                                        )
                                )
                        ),
                        transactions,
                        new PaymentConfirmationReplayCodec(),
                        time
                );

        PaymentConfirmationBankResult recovered =
                new PaymentConfirmationBankResult(
                        new ConfirmationChallengeReference("CHALLENGE-NEW"),
                        ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.CHALLENGE_REPLACED,
                        null,
                        Instant.parse("2026-09-16T20:00:02Z"),
                        Instant.parse("2026-09-16T20:05:02Z"),
                        null);

        AtomicInteger replaceCalls = new AtomicInteger();
        AtomicInteger recoveryCalls = new AtomicInteger();

        var actual = adapter.executeReplace(
                paymentId, paymentReference, oldChallenge, key,
                () -> {
                    replaceCalls.incrementAndGet();
                    throw new PaymentConfirmationGateway.OutcomeUnknownException(
                            "resend response timeout", null, null);
                },
                () -> {
                    recoveryCalls.incrementAndGet();
                    return recovered;
                });

        assertThat(actual.result()).isEqualTo(recovered);
        assertThat(replaceCalls.get()).isEqualTo(1);
        assertThat(recoveryCalls.get()).isEqualTo(1);
        verify(transactions).markOutcomeUnknown(
                eq(PaymentConfirmationIdempotencyAdapter.REPLACE_OPERATION),
                eq(key.value()), eq(requestHash), eq(paymentId.value()),
                isNull(), eq("resend response timeout"), any(Instant.class));
    }
}
