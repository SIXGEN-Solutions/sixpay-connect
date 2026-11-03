package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentConfirmationOptionalHmacTest {
    private static final PaymentId PAYMENT_ID = new PaymentId(
            UUID.fromString("11111111-1111-4111-8111-111111111111"));
    private static final PublicPaymentReference PAYMENT_REFERENCE =
            PublicPaymentReference.of("PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV");

    private final PaymentConfirmationIdempotencyTransactions transactions =
            mock(PaymentConfirmationIdempotencyTransactions.class);
    private final TimeProvider clock = () -> Instant.parse("2026-08-30T12:00:00Z");
    private final PaymentConfirmationIdempotencyAdapter adapter =
            new PaymentConfirmationIdempotencyAdapter(
                    new PaymentConfirmationCanonicalizer(),
                    new PaymentIdempotencyHasher(),
                    (PaymentOtpIdempotencyFingerprintSet) null,
                    transactions,
                    new PaymentConfirmationReplayCodec(), clock);

    @Test
    void createWithoutHmacStillUsesIdempotencyAndBankGateway() {
        var key = IdempotencyKey.of("create-no-hmac-001");
        var expectedHash = new PaymentIdempotencyHasher().hash(
                new PaymentConfirmationCanonicalizer().create(PAYMENT_ID, PAYMENT_REFERENCE));
        when(transactions.begin(eq("PAYMENT_CONFIRMATION_CREATE"),
                eq(key.value()), eq(expectedHash), eq(List.of(expectedHash)), any(Instant.class)))
                .thenReturn(new PaymentConfirmationIdempotencyTransactions.BeginResult(
                        PaymentIdempotencyDecision.newRequest(), expectedHash));
        PaymentConfirmationBankResult bankResult = mock(PaymentConfirmationBankResult.class);
        // A non-null result is enough to exercise the create path up to complete().
        // Use a real result to validate persistence replay formatting.
        when(bankResult.challengeReference()).thenReturn(
                new com.sixpay.payment.domain.model.ConfirmationChallengeReference("challenge-local"));
        when(bankResult.status()).thenReturn(
                com.sixpay.payment.domain.model.ConfirmationChallengeStatus.ACTIVE);
        when(bankResult.businessCode()).thenReturn(
                com.sixpay.payment.domain.model.ConfirmationBusinessCode.CHALLENGE_ACTIVE);
        AtomicInteger calls = new AtomicInteger();
        var result = adapter.executeCreate(PAYMENT_ID, PAYMENT_REFERENCE, key,
                () -> { calls.incrementAndGet(); return bankResult; },
                () -> { throw new AssertionError("Unexpected recovery"); });
        assertFalse(result.replayed());
        assertEquals(1, calls.get());
        verify(transactions).complete(eq("PAYMENT_CONFIRMATION_CREATE"),
                eq(key.value()), eq(expectedHash), eq(PAYMENT_ID.value()),
                eq("ACTIVE"), anyString(), any(Instant.class));
    }

    @Test
    void verifyWithoutHmacIsRejectedBeforeBankAndPersistence() {
        AtomicInteger calls = new AtomicInteger();
        var failure = assertThrows(IllegalStateException.class,
                () -> adapter.executeVerify(PAYMENT_ID, PAYMENT_REFERENCE,
                        IdempotencyKey.of("verify-no-hmac-001"),
                        "123456".toCharArray(),
                        () -> { calls.incrementAndGet(); return null; }));
        assertTrue(failure.getMessage().contains("HMAC"));
        assertEquals(0, calls.get());
        verifyNoInteractions(transactions);
    }
}
