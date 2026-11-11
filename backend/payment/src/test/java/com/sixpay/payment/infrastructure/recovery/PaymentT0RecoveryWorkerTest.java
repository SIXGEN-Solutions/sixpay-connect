package com.sixpay.payment.infrastructure.recovery;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.input.RecoverUnknownPaymentT0UseCase;
import com.sixpay.payment.application.service.PaymentWorkflowResult;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentT0RecoveryWorkerTest {
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final UUID ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final PaymentT0RecoveryClaimStore.Claim CLAIM =
            new PaymentT0RecoveryClaimStore.Claim(ID,
                    UUID.fromString("22222222-2222-4222-8222-222222222222"), 1);

    @Test
    void uncertainResultSchedulesBackoffWithoutPosting() {
        var store = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var time = mock(TimeProvider.class);
        when(time.now()).thenReturn(NOW);
        when(store.claim(any(), any())).thenReturn(List.of(CLAIM));
        when(recovery.recoverByPaymentId(any(), any())).thenReturn(result(PaymentStatus.POSTING_OUTCOME_UNKNOWN));
        new PaymentT0RecoveryWorker(store, recovery, props(), time).reconcileAvailable();
        verify(store).retry(CLAIM, NOW, Duration.ofMinutes(2));
        verify(store, never()).completed(any(), any());
        verify(recovery).recoverByPaymentId(any(), any());
        verifyNoMoreInteractions(recovery);
    }

    @Test
    void resolvedResultClosesClaim() {
        var store = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var time = mock(TimeProvider.class);
        when(time.now()).thenReturn(NOW);
        when(store.claim(any(), any())).thenReturn(List.of(CLAIM));
        when(recovery.recoverByPaymentId(any(), any())).thenReturn(result(PaymentStatus.POSTED_PENDING_TFJ));
        new PaymentT0RecoveryWorker(store, recovery, props(), time).reconcileAvailable();
        verify(store).completed(CLAIM, NOW);
        verify(store, never()).retry(any(), any(), any());
    }

    @Test
    void technicalFailureRetriesWithoutConcludingRejection() {
        var store = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var time = mock(TimeProvider.class);
        when(time.now()).thenReturn(NOW);
        when(store.claim(any(), any())).thenReturn(List.of(CLAIM));
        when(recovery.recoverByPaymentId(any(), any())).thenThrow(new IllegalStateException("bank unavailable"));
        new PaymentT0RecoveryWorker(store, recovery, props(), time).reconcileAvailable();
        verify(store).retry(CLAIM, NOW, Duration.ofMinutes(2));
        verify(store, never()).completed(any(), any());
    }

    @Test
    void cappedBackoffDoesNotIncreaseUnboundedly() {
        assertEquals(Duration.ofMinutes(2), PaymentT0RecoveryWorker.delay(1));
        assertEquals(Duration.ofMinutes(5), PaymentT0RecoveryWorker.delay(2));
        assertEquals(Duration.ofMinutes(15), PaymentT0RecoveryWorker.delay(3));
        assertEquals(Duration.ofMinutes(30), PaymentT0RecoveryWorker.delay(4));
        assertEquals(Duration.ofMinutes(60), PaymentT0RecoveryWorker.delay(5));
        assertEquals(Duration.ofMinutes(60), PaymentT0RecoveryWorker.delay(100));
    }

    private static PaymentT0RecoveryBatchProperties props() {
        return new PaymentT0RecoveryBatchProperties();
    }

    private static PaymentWorkflowResult result(PaymentStatus status) {
        return new PaymentWorkflowResult(new PaymentId(ID),
                PublicPaymentReference.of("PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"), status, 1, true);
    }
}
