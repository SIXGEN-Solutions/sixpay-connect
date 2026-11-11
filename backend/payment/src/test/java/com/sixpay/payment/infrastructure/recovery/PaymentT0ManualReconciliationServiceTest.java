package com.sixpay.payment.infrastructure.recovery;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.input.RecoverUnknownPaymentT0UseCase;
import com.sixpay.payment.application.port.output.security.PaymentObjectAccessPort;
import com.sixpay.payment.application.security.PaymentAccessPolicy;
import com.sixpay.payment.application.security.PaymentAction;
import com.sixpay.payment.application.security.PaymentObjectAccessDescriptor;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentT0ManualReconciliationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final UUID PAYMENT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CORRELATION = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Test
    void selectionCannotBypassExistingObjectAuthorization() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var access = mock(PaymentObjectAccessPort.class);
        var policy = mock(PaymentAccessPolicy.class);
        var users = mock(CurrentUserProvider.class);
        var actor = mock(AuthenticatedUser.class);
        var audit = mock(PaymentT0ManualAuditStore.class);
        var clock = mock(TimeProvider.class);
        when(users.requireCurrentUser()).thenReturn(actor);
        when(actor.subject()).thenReturn("ops-1");
        when(clock.now()).thenReturn(NOW);
        when(access.findAccessDescriptor(new PaymentId(PAYMENT))).thenReturn(Optional.of(
                new PaymentObjectAccessDescriptor(new PaymentId(PAYMENT), PaymentSource.TRESOR_PAY, null)));
        when(claims.claimSelected(eq(PAYMENT), eq(NOW), any(), startsWith("manual:")))
                .thenReturn(Optional.empty());
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), recovery, access, policy, users, audit, clock);
        var result = service.reconcile(List.of(PAYMENT), CORRELATION);
        assertEquals("NOT_ELIGIBLE_OR_BUSY", result.getFirst().outcome());
        verify(policy).requireObjectAccess(eq(PaymentAction.RECONCILE), any());
        verify(audit).record(PAYMENT, "ops-1", CORRELATION, "NOT_ELIGIBLE_OR_BUSY", NOW);
        verifyNoInteractions(recovery);
    }

    @Test
    void nullPaymentIdIsRejectedBeforeClaims() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), mock(RecoverUnknownPaymentT0UseCase.class),
                mock(PaymentObjectAccessPort.class), mock(PaymentAccessPolicy.class),
                mock(CurrentUserProvider.class), mock(PaymentT0ManualAuditStore.class),
                mock(TimeProvider.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.reconcile(java.util.Arrays.asList(PAYMENT, null), CORRELATION));
        verifyNoInteractions(claims);
    }

    @Test
    void duplicateSelectionIsRejectedBeforeClaims() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), mock(RecoverUnknownPaymentT0UseCase.class),
                mock(PaymentObjectAccessPort.class), mock(PaymentAccessPolicy.class),
                mock(CurrentUserProvider.class), mock(PaymentT0ManualAuditStore.class),
                mock(TimeProvider.class));
        assertThrows(IllegalArgumentException.class,
                () -> service.reconcile(List.of(PAYMENT, PAYMENT), CORRELATION));
        verifyNoInteractions(claims);
    }
    @Test
    void successfulRecoveryReusesExistingPortAndCompletesOwnedClaim() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var access = mock(PaymentObjectAccessPort.class);
        var policy = mock(PaymentAccessPolicy.class);
        var users = mock(CurrentUserProvider.class);
        var actor = mock(AuthenticatedUser.class);
        var audit = mock(PaymentT0ManualAuditStore.class);
        var clock = mock(TimeProvider.class);
        var token = new PaymentT0RecoveryClaimStore.Claim(PAYMENT,
                UUID.fromString("33333333-3333-4333-8333-333333333333"), 1);
        when(users.requireCurrentUser()).thenReturn(actor);
        when(actor.subject()).thenReturn("ops-1");
        when(clock.now()).thenReturn(NOW);
        when(access.findAccessDescriptor(new PaymentId(PAYMENT))).thenReturn(Optional.of(
                new PaymentObjectAccessDescriptor(new PaymentId(PAYMENT), PaymentSource.TRESOR_PAY, null)));
        when(claims.claimSelected(eq(PAYMENT), eq(NOW), any(), startsWith("manual:")))
                .thenReturn(Optional.of(token));
        when(claims.completed(token, NOW)).thenReturn(true);
        when(recovery.recoverByPaymentId(eq(new PaymentId(PAYMENT)), any())).thenReturn(
                new com.sixpay.payment.application.service.PaymentWorkflowResult(
                        new PaymentId(PAYMENT),
                        com.sixpay.payment.domain.model.PublicPaymentReference.of(
                                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        com.sixpay.payment.domain.model.PaymentStatus.POSTED_PENDING_TFJ, 2, true));
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), recovery, access, policy, users, audit, clock);
        var result = service.reconcile(List.of(PAYMENT), CORRELATION);
        assertEquals("RESOLVED", result.getFirst().outcome());
        verify(claims).completed(token, NOW);
        verify(claims, never()).retry(any(), any(), any());
        verify(audit).record(PAYMENT, "ops-1", CORRELATION, "RESOLVED", NOW);
    }

    @Test
    void deniedObjectIsAuditedAndNeverClaimed() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var access = mock(PaymentObjectAccessPort.class);
        var policy = mock(PaymentAccessPolicy.class);
        var users = mock(CurrentUserProvider.class);
        var actor = mock(AuthenticatedUser.class);
        var audit = mock(PaymentT0ManualAuditStore.class);
        var clock = mock(TimeProvider.class);
        when(users.requireCurrentUser()).thenReturn(actor);
        when(actor.subject()).thenReturn("ops-1");
        when(clock.now()).thenReturn(NOW);
        when(access.findAccessDescriptor(new PaymentId(PAYMENT))).thenReturn(Optional.empty());
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), recovery, access, policy, users, audit, clock);
        assertThrows(com.sixpay.payment.application.security.PaymentAccessDeniedException.class,
                () -> service.reconcile(List.of(PAYMENT), CORRELATION));
        verify(audit).record(PAYMENT, "ops-1", CORRELATION, "DENIED", NOW);
        verifyNoInteractions(claims, recovery);
    }

    // BEGIN SIXPAY_R6_T0_VALIDATION_TESTS
    @Test
    void r6ConclusiveCompletedBankLookupResolvesOwnedClaim() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.POSTED_PENDING_TFJ));
        when(fixture.claims().completed(fixture.claim(), NOW)).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals(1, results.size());
        assertEquals("RESOLVED", results.getFirst().outcome());
        assertEquals("POSTED_PENDING_TFJ", results.getFirst().paymentStatus());
        verify(fixture.claims()).completed(fixture.claim(), NOW);
        verify(fixture.claims(), never()).retry(any(), any(), any());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION, "RESOLVED", NOW);
    }

    @Test
    void r6ConclusiveRejectedBankLookupResolvesOwnedClaimWithoutRetry() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.REJECTED));
        when(fixture.claims().completed(fixture.claim(), NOW)).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("RESOLVED", results.getFirst().outcome());
        assertEquals("REJECTED", results.getFirst().paymentStatus());
        verify(fixture.claims()).completed(fixture.claim(), NOW);
        verify(fixture.claims(), never()).retry(any(), any(), any());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION, "RESOLVED", NOW);
    }

    @Test
    void r6StillUnknownBankLookupKeepsTheAttemptRetryable() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.POSTING_OUTCOME_UNKNOWN));
        when(fixture.claims().retry(eq(fixture.claim()), eq(NOW), any())).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("RETRY_SCHEDULED", results.getFirst().outcome());
        assertEquals("POSTING_OUTCOME_UNKNOWN", results.getFirst().paymentStatus());
        verify(fixture.claims(), never()).completed(any(), any());
        verify(fixture.claims()).retry(fixture.claim(), NOW,
                java.time.Duration.ofMinutes(2));
    }

    @Test
    void r6UnavailableBankLookupSchedulesRetryWithoutFalseRejection() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenThrow(new IllegalStateException("Core Banking unavailable"));
        when(fixture.claims().retry(eq(fixture.claim()), eq(NOW), any())).thenReturn(true);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("LOOKUP_FAILED_RETRY_SCHEDULED", results.getFirst().outcome());
        assertNull(results.getFirst().paymentStatus());
        verify(fixture.claims(), never()).completed(any(), any());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION,
                "LOOKUP_FAILED_RETRY_SCHEDULED", NOW);
    }

    @Test
    void r6OldFencingTokenCannotReportSuccessfulResolution() {
        var fixture = r6Fixture();
        when(fixture.recovery().recoverByPaymentId(eq(new PaymentId(PAYMENT)), any()))
                .thenReturn(r6Result(com.sixpay.payment.domain.model.PaymentStatus.REJECTED));
        when(fixture.claims().completed(fixture.claim(), NOW)).thenReturn(false);

        var results = fixture.service().reconcile(List.of(PAYMENT), CORRELATION);

        assertEquals("STALE_CLAIM", results.getFirst().outcome());
        verify(fixture.audit()).record(PAYMENT, "r6-admin", CORRELATION, "STALE_CLAIM", NOW);
    }

    private static com.sixpay.payment.application.service.PaymentWorkflowResult r6Result(
            com.sixpay.payment.domain.model.PaymentStatus status) {
        return new com.sixpay.payment.application.service.PaymentWorkflowResult(
                new PaymentId(PAYMENT),
                com.sixpay.payment.domain.model.PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"), status, 2, true);
    }

    private record R6Fixture(
            PaymentT0ManualReconciliationService service,
            PaymentT0RecoveryClaimStore claims,
            RecoverUnknownPaymentT0UseCase recovery,
            PaymentT0ManualAuditStore audit,
            PaymentT0RecoveryClaimStore.Claim claim) { }

    private static R6Fixture r6Fixture() {
        var claims = mock(PaymentT0RecoveryClaimStore.class);
        var recovery = mock(RecoverUnknownPaymentT0UseCase.class);
        var access = mock(PaymentObjectAccessPort.class);
        var policy = mock(PaymentAccessPolicy.class);
        var users = mock(CurrentUserProvider.class);
        var actor = mock(AuthenticatedUser.class);
        var audit = mock(PaymentT0ManualAuditStore.class);
        var time = mock(TimeProvider.class);
        var claim = new PaymentT0RecoveryClaimStore.Claim(PAYMENT,
                UUID.fromString("33333333-3333-4333-8333-333333333333"), 1);
        when(users.requireCurrentUser()).thenReturn(actor);
        when(actor.subject()).thenReturn("r6-admin");
        when(time.now()).thenReturn(NOW);
        when(access.findAccessDescriptor(new PaymentId(PAYMENT)))
                .thenReturn(Optional.of(new PaymentObjectAccessDescriptor(
                        new PaymentId(PAYMENT), PaymentSource.TRESOR_PAY, null)));
        when(claims.claimSelected(eq(PAYMENT), eq(NOW), any(), startsWith("manual:")))
                .thenReturn(Optional.of(claim));
        var service = new PaymentT0ManualReconciliationService(claims,
                new PaymentT0RecoveryBatchProperties(), recovery, access, policy, users,
                audit, time);
        return new R6Fixture(service, claims, recovery, audit, claim);
    }
    // END SIXPAY_R6_T0_VALIDATION_TESTS

}
