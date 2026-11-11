package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.PaymentTreasuryAccountResolutionPort;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentT05OrchestrationSafetyTest {
    private static final PaymentId ID = new PaymentId(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
    private static final Instant AT = Instant.parse("2026-09-06T18:00:00Z");
    private static final CorrelationId CORRELATION = CorrelationId.of("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    @Test
    void rejectedOrPrematureFundsEventDoesNotAdvanceWorkflow() {
        Fixture f = fixture(PaymentStatus.REJECTED);
        f.service.onFundsControlRequested(ID);
        verifyNoInteractions(f.funds);
        verifyNoInteractions(f.treasury);
        verifyNoInteractions(f.snapshots);
        verifyNoInteractions(f.lifecycle);
    }

    @Test
    void validFundsRequestAdvancesPreparationExactlyOncePerInvocation() {
        Fixture f = fixture(PaymentStatus.FUNDS_CONTROL_PENDING);
        f.service.onFundsControlRequested(ID);
        verify(f.funds, times(1)).completeFundsControlPreparation(ID, AT);
        verifyNoInteractions(f.treasury, f.snapshots, f.lifecycle);
    }

    @Test
    void unavailableTreasuryBridgeDoesNotPersistResolutionOrPost() {
        Fixture f = fixture(PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING);
        assertThrows(IllegalStateException.class,
                () -> f.service.onTreasuryResolutionRequested(ID, CORRELATION));
        verifyNoInteractions(f.treasury, f.snapshots, f.lifecycle);
    }

    @Test
    void treasuryTimeoutDoesNotPersistResolutionOrPost() {
        Fixture f = fixture(PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING);
        PaymentTreasuryAccountResolutionPort resolver = mock(PaymentTreasuryAccountResolutionPort.class);
        when(f.treasuryProvider.getIfAvailable()).thenReturn(resolver);
        when(resolver.resolve(f.payment, AT)).thenThrow(new IllegalStateException("treasury timeout"));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> f.service.onTreasuryResolutionRequested(ID, CORRELATION));
        assertEquals("treasury timeout", failure.getMessage());
        verify(resolver, times(1)).resolve(f.payment, AT);
        verifyNoInteractions(f.treasury, f.snapshots, f.lifecycle);
    }

    @Test
    void nonApprovedPaymentNeverCreatesSnapshotOrExecutesPosting() {
        Fixture f = fixture(PaymentStatus.TREASURY_ACCOUNT_RESOLUTION_PENDING);
        f.service.onApprovedForPosting(ID, CORRELATION);
        verifyNoInteractions(f.snapshots, f.lifecycle);
    }

    @SuppressWarnings("unchecked")
    private static Fixture fixture(PaymentStatus status) {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        PaymentFundsControlService funds = mock(PaymentFundsControlService.class);
        PaymentTreasuryResolutionService treasury = mock(PaymentTreasuryResolutionService.class);
        ObjectProvider<PaymentTreasuryAccountResolutionPort> treasuryProvider = mock(ObjectProvider.class);
        PaymentT0IdentityFactory identityFactory = mock(PaymentT0IdentityFactory.class);
        PaymentT0SnapshotService snapshots = mock(PaymentT0SnapshotService.class);
        ObjectProvider<PaymentEventLifecycleOrchestrationService> lifecycleProvider = mock(ObjectProvider.class);
        PaymentEventLifecycleOrchestrationService lifecycle = mock(PaymentEventLifecycleOrchestrationService.class);
        PaymentPolicyBundle policies = mock(PaymentPolicyBundle.class);
        TimeProvider time = () -> AT;
        Payment payment = mock(Payment.class);
        when(lookup.findById(ID)).thenReturn(Optional.of(payment));
        when(payment.status()).thenReturn(status);
        PaymentT0OrchestrationService service = new PaymentT0OrchestrationService(
                lookup, funds, treasury, treasuryProvider, identityFactory, snapshots,
                lifecycleProvider, policies, time);
        return new Fixture(service, payment, funds, treasury, treasuryProvider, snapshots, lifecycle);
    }

    private record Fixture(PaymentT0OrchestrationService service, Payment payment,
            PaymentFundsControlService funds, PaymentTreasuryResolutionService treasury,
            ObjectProvider<PaymentTreasuryAccountResolutionPort> treasuryProvider,
            PaymentT0SnapshotService snapshots, PaymentEventLifecycleOrchestrationService lifecycle) { }
}
