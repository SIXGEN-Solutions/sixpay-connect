package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentT0RecoveryServiceTest {

    @Mock
    private PaymentLookupPort paymentLookupPort;

    @Mock
    private ObjectProvider<PaymentEventLifecycleOrchestrationService>
            lifecycleProvider;

    @Mock
    private PaymentEventLifecycleOrchestrationService lifecycle;

    @Mock
    private PaymentPolicyBundle policies;

    @Mock
    private TimeProvider timeProvider;

    @Test
    void durableUnknownPaymentDelegatesOnlyToRecoveryPath() {
        Payment payment = mock(Payment.class);
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        CorrelationId correlationId =
                CorrelationId.of(UUID.randomUUID().toString());
        Instant now = Instant.parse("2026-09-07T04:30:00Z");

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));
        when(payment.status())
                .thenReturn(PaymentStatus.POSTING_OUTCOME_UNKNOWN);
        when(payment.id()).thenReturn(paymentId);
        com.sixpay.payment.domain.model.PaymentState state =
                mock(com.sixpay.payment.domain.model.PaymentState.class);

        when(state.financialInstitutionCode())
                .thenReturn(
                        com.sixpay.payment.domain.model
                                .FinancialInstitutionCode.of("LRB")
                );
        when(payment.toState()).thenReturn(state);
        when(lifecycleProvider.getIfAvailable()).thenReturn(lifecycle);
        when(timeProvider.now()).thenReturn(now);

        service().recoverByPaymentId(paymentId, correlationId);

        verify(lifecycle).recover(
                eq(paymentId),
                any(),
                eq(now),
                eq(policies)
        );
    }

    @Test
    void nonUnknownPaymentIsRejectedBeforeRecoveryLookup() {
        Payment payment = mock(Payment.class);
        PaymentId paymentId = new PaymentId(UUID.randomUUID());

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));
        when(payment.status()).thenReturn(PaymentStatus.REJECTED);

        assertThatThrownBy(
                () -> service().recoverByPaymentId(
                        paymentId,
                        CorrelationId.of(
                                UUID.randomUUID().toString()
                        )
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "POSTING_OUTCOME_UNKNOWN"
                );
    }

    private PaymentT0RecoveryService service() {
        return new PaymentT0RecoveryService(
                paymentLookupPort,
                lifecycleProvider,
                policies,
                timeProvider
        );
    }
}
