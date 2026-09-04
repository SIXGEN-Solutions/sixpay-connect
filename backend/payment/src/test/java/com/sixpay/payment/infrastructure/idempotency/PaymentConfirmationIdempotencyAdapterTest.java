package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
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

class PaymentConfirmationIdempotencyAdapterTest {

    private static final PublicPaymentReference PAYMENT_REFERENCE =
            PublicPaymentReference.of(
                    "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
            );

    @Test
    void completedVerifyReplayNeverInvokesBankVerification() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        )
                );
        IdempotencyKey key =
                IdempotencyKey.of("verify-key-0001");

        PaymentOtpIdempotencyFingerprint fingerprint =
                fingerprint("current-key-material-for-verify");
        PaymentOtpIdempotencyFingerprintSet fingerprintSet =
                new PaymentOtpIdempotencyFingerprintSet(
                        List.of(fingerprint)
                );

        String requestHash =
                fingerprint.fingerprint(
                        PAYMENT_REFERENCE.value(),
                        "123456"
                );

        PaymentConfirmationBankResult completed =
                result("challenge-verify");

        PaymentConfirmationReplayCodec codec =
                new PaymentConfirmationReplayCodec();

        PaymentIdempotencyDecision replay =
                new PaymentIdempotencyDecision(
                        PaymentIdempotencyDecision.Kind.REPLAY,
                        paymentId.value(),
                        completed.status().name(),
                        codec.encode(completed),
                        null,
                        null,
                        null
                );

        PaymentConfirmationIdempotencyTransactions transactions =
                mock(PaymentConfirmationIdempotencyTransactions.class);

        when(transactions.begin(
                eq(PaymentConfirmationIdempotencyAdapter.VERIFY_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(List.of(requestHash)),
                any(Instant.class)
        )).thenReturn(
                new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        replay,
                        requestHash
                )
        );

        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(
                Instant.parse("2026-08-30T12:00:00Z")
        );

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        new PaymentConfirmationCanonicalizer(),
                        new PaymentIdempotencyHasher(),
                        fingerprintSet,
                        transactions,
                        codec,
                        timeProvider
                );

        AtomicInteger bankCalls = new AtomicInteger();

        PaymentConfirmationIdempotencyResult actual =
                adapter.executeVerify(
                        paymentId,
                        PAYMENT_REFERENCE,
                        key,
                        "123456".toCharArray(),
                        () -> {
                            bankCalls.incrementAndGet();
                            return completed;
                        }
                );

        assertThat(actual.result()).isEqualTo(completed);
        assertThat(actual.replayed()).isTrue();
        assertThat(bankCalls.get()).isZero();
    }

    @Test
    void uncertainCreateMarksUnknownThenRecoversWithoutBlindRetry() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.fromString(
                                "22222222-2222-4222-8222-222222222222"
                        )
                );
        IdempotencyKey key =
                IdempotencyKey.of("create-key-0001");

        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher =
                new PaymentIdempotencyHasher();

        String requestHash =
                hasher.hash(
                        canonicalizer.create(
                                paymentId,
                                PAYMENT_REFERENCE
                        )
                );

        PaymentConfirmationIdempotencyTransactions transactions =
                mock(PaymentConfirmationIdempotencyTransactions.class);

        when(transactions.begin(
                eq(PaymentConfirmationIdempotencyAdapter.CREATE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(List.of(requestHash)),
                any(Instant.class)
        )).thenReturn(
                new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        PaymentIdempotencyDecision.newRequest(),
                        requestHash
                )
        );

        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(
                Instant.parse("2026-08-30T12:00:00Z"),
                Instant.parse("2026-08-30T12:00:01Z"),
                Instant.parse("2026-08-30T12:00:02Z")
        );

        PaymentConfirmationBankResult recovered =
                result("challenge-recovered");

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        new PaymentOtpIdempotencyFingerprintSet(
                                List.of(
                                        fingerprint(
                                                "unused-key-material"
                                        )
                                )
                        ),
                        transactions,
                        new PaymentConfirmationReplayCodec(),
                        timeProvider
                );

        AtomicInteger createCalls = new AtomicInteger();
        AtomicInteger recoveryCalls = new AtomicInteger();

        PaymentConfirmationIdempotencyResult actual =
                adapter.executeCreate(
                        paymentId,
                        PAYMENT_REFERENCE,
                        key,
                        () -> {
                            createCalls.incrementAndGet();
                            throw new PaymentConfirmationGateway
                                    .OutcomeUnknownException(
                                            "create response timeout",
                                            null,
                                            null
                                    );
                        },
                        () -> {
                            recoveryCalls.incrementAndGet();
                            return recovered;
                        }
                );

        assertThat(actual.result()).isEqualTo(recovered);
        assertThat(actual.replayed()).isFalse();
        assertThat(createCalls.get()).isEqualTo(1);
        assertThat(recoveryCalls.get()).isEqualTo(1);

        verify(transactions).markOutcomeUnknown(
                eq(PaymentConfirmationIdempotencyAdapter.CREATE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(paymentId.value()),
                isNull(),
                eq("create response timeout"),
                any(Instant.class)
        );
    }

    @Test
    void completedRevokeReplayNeverInvokesBankRevocation() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.fromString(
                                "55555555-5555-4555-8555-555555555555"
                        )
                );
        IdempotencyKey key =
                IdempotencyKey.of("revoke-key-0001");
        ConfirmationChallengeReference challengeReference =
                new ConfirmationChallengeReference(
                        "challenge-revoke"
                );

        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher =
                new PaymentIdempotencyHasher();

        String requestHash =
                hasher.hash(
                        canonicalizer.revoke(
                                paymentId,
                                PAYMENT_REFERENCE,
                                challengeReference,
                                "PAYMENT_REJECTED"
                        )
                );

        PaymentConfirmationBankResult completed =
                resultWithStatus(
                        "challenge-revoke",
                        ConfirmationChallengeStatus.REVOKED,
                        ConfirmationBusinessCode.CHALLENGE_REVOKED
                );

        PaymentConfirmationReplayCodec codec =
                new PaymentConfirmationReplayCodec();

        PaymentIdempotencyDecision replay =
                new PaymentIdempotencyDecision(
                        PaymentIdempotencyDecision.Kind.REPLAY,
                        paymentId.value(),
                        completed.status().name(),
                        codec.encode(completed),
                        null,
                        null,
                        null
                );

        PaymentConfirmationIdempotencyTransactions transactions =
                mock(PaymentConfirmationIdempotencyTransactions.class);

        when(transactions.begin(
                eq(PaymentConfirmationIdempotencyAdapter.REVOKE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(List.of(requestHash)),
                any(Instant.class)
        )).thenReturn(
                new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        replay,
                        requestHash
                )
        );

        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(
                Instant.parse("2026-08-31T22:00:00Z")
        );

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        new PaymentOtpIdempotencyFingerprintSet(
                                List.of(
                                        fingerprint(
                                                "unused-revoke-key-material"
                                        )
                                )
                        ),
                        transactions,
                        codec,
                        timeProvider
                );

        AtomicInteger revokeCalls = new AtomicInteger();

        PaymentConfirmationIdempotencyResult actual =
                adapter.executeRevoke(
                        paymentId,
                        PAYMENT_REFERENCE,
                        challengeReference,
                        key,
                        "PAYMENT_REJECTED",
                        () -> {
                            revokeCalls.incrementAndGet();
                            return completed;
                        },
                        () -> {
                            throw new AssertionError(
                                    "Recovery must not run on completed replay"
                            );
                        }
                );

        assertThat(actual.result()).isEqualTo(completed);
        assertThat(actual.replayed()).isTrue();
        assertThat(revokeCalls.get()).isZero();
    }

    @Test
    void uncertainRevokeMarksUnknownThenRecoversWithoutBlindRetry() {
        PaymentId paymentId =
                new PaymentId(
                        UUID.fromString(
                                "66666666-6666-4666-8666-666666666666"
                        )
                );
        IdempotencyKey key =
                IdempotencyKey.of("revoke-key-0002");
        ConfirmationChallengeReference challengeReference =
                new ConfirmationChallengeReference(
                        "challenge-revoke-unknown"
                );

        PaymentConfirmationCanonicalizer canonicalizer =
                new PaymentConfirmationCanonicalizer();
        PaymentIdempotencyHasher hasher =
                new PaymentIdempotencyHasher();

        String requestHash =
                hasher.hash(
                        canonicalizer.revoke(
                                paymentId,
                                PAYMENT_REFERENCE,
                                challengeReference,
                                "PAYMENT_FAILED"
                        )
                );

        PaymentConfirmationIdempotencyTransactions transactions =
                mock(PaymentConfirmationIdempotencyTransactions.class);

        when(transactions.begin(
                eq(PaymentConfirmationIdempotencyAdapter.REVOKE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(List.of(requestHash)),
                any(Instant.class)
        )).thenReturn(
                new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        PaymentIdempotencyDecision.newRequest(),
                        requestHash
                )
        );

        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(
                Instant.parse("2026-08-31T22:10:00Z"),
                Instant.parse("2026-08-31T22:10:01Z"),
                Instant.parse("2026-08-31T22:10:02Z")
        );

        PaymentConfirmationBankResult recovered =
                resultWithStatus(
                        "challenge-revoke-unknown",
                        ConfirmationChallengeStatus.REVOKED,
                        ConfirmationBusinessCode.CHALLENGE_REVOKED
                );

        PaymentConfirmationIdempotencyAdapter adapter =
                new PaymentConfirmationIdempotencyAdapter(
                        canonicalizer,
                        hasher,
                        new PaymentOtpIdempotencyFingerprintSet(
                                List.of(
                                        fingerprint(
                                                "unused-revoke-key-material"
                                        )
                                )
                        ),
                        transactions,
                        new PaymentConfirmationReplayCodec(),
                        timeProvider
                );

        AtomicInteger revokeCalls = new AtomicInteger();
        AtomicInteger recoveryCalls = new AtomicInteger();

        PaymentConfirmationIdempotencyResult actual =
                adapter.executeRevoke(
                        paymentId,
                        PAYMENT_REFERENCE,
                        challengeReference,
                        key,
                        "PAYMENT_FAILED",
                        () -> {
                            revokeCalls.incrementAndGet();
                            throw new PaymentConfirmationGateway
                                    .OutcomeUnknownException(
                                            "revoke response timeout",
                                            key.value(),
                                            null
                                    );
                        },
                        () -> {
                            recoveryCalls.incrementAndGet();
                            return recovered;
                        }
                );

        assertThat(actual.result()).isEqualTo(recovered);
        assertThat(actual.replayed()).isFalse();
        assertThat(revokeCalls.get()).isEqualTo(1);
        assertThat(recoveryCalls.get()).isEqualTo(1);

        verify(transactions).markOutcomeUnknown(
                eq(PaymentConfirmationIdempotencyAdapter.REVOKE_OPERATION),
                eq(key.value()),
                eq(requestHash),
                eq(paymentId.value()),
                eq(key.value()),
                eq("revoke response timeout"),
                any(Instant.class)
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

    private static PaymentConfirmationBankResult result(
            String challengeReference
    ) {
        return resultWithStatus(
                challengeReference,
                ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_ACTIVE
        );
    }

    private static PaymentConfirmationBankResult resultWithStatus(
            String challengeReference,
            ConfirmationChallengeStatus status,
            ConfirmationBusinessCode businessCode
    ) {
        return new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference(
                        challengeReference
                ),
                status,
                businessCode,
                null,
                null,
                null,
                null
        );
    }
}
