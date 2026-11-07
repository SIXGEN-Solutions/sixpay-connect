package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class PaymentT06PendingRecoveryTest {
    @Test
    void pendingPaymentUsesLookupOnlyWithoutAnyFinancialPost() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PaymentEventLifecycleOrchestrationService> provider =
                mock(ObjectProvider.class);
        PaymentEventLifecycleOrchestrationService lifecycle =
                mock(PaymentEventLifecycleOrchestrationService.class);
        PaymentPolicyBundle policies = mock(PaymentPolicyBundle.class);
        TimeProvider clock = mock(TimeProvider.class);
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        PaymentId id = new PaymentId(UUID.randomUUID());
        CorrelationId correlation = CorrelationId.of(UUID.randomUUID().toString());
        Instant now = Instant.parse("2026-09-07T12:00:00Z");

        when(lookup.findById(id)).thenReturn(Optional.of(payment));
        when(payment.status()).thenReturn(PaymentStatus.POSTING_PENDING);
        when(payment.id()).thenReturn(id);
        when(payment.toState()).thenReturn(state);
        when(state.financialInstitutionCode()).thenReturn(FinancialInstitutionCode.of("LRB"));
        when(provider.getIfAvailable()).thenReturn(lifecycle);
        when(clock.now()).thenReturn(now);

        PaymentT0RecoveryService service = new PaymentT0RecoveryService(
                lookup, provider, policies, clock);
        service.recoverByPaymentId(id, correlation);

        verify(lifecycle).recover(eq(id), any(), eq(now), eq(policies));
        verify(lifecycle, never()).execute(any(), any(), any(), any(), any());
    }
}
