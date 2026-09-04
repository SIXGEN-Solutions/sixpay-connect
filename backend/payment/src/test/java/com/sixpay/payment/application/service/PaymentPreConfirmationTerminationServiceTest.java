package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentPreConfirmationTerminationServiceTest {

    @Test
    void rejectFinalizesOnlyAfterStableRevocation() {
        PaymentConfirmationRevocationService revocationService =
                mock(PaymentConfirmationRevocationService.class);
        PaymentFinalizationService finalizationService =
                mock(PaymentFinalizationService.class);

        PaymentConfirmationRevocationService.RevocationResult revocation =
                mock(PaymentConfirmationRevocationService.RevocationResult.class);
        when(revocation.stableForTerminalTransition())
                .thenReturn(true);
        when(revocationService.revokeActiveChallenge(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(revocation);

        PaymentWorkflowResult expected =
                new PaymentWorkflowResult(
                        mock(PaymentId.class),
                        mock(
                                com.sixpay.payment.domain.model
                                        .PublicPaymentReference.class
                        ),
                        PaymentStatus.REJECTED,
                        7L,
                        true
                );
        when(finalizationService.rejectAfterPreConfirmationRevocation(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(expected);

        PaymentPreConfirmationTerminationService service =
                new PaymentPreConfirmationTerminationService(
                        revocationService,
                        finalizationService
                );

        PaymentWorkflowResult actual =
                service.reject(
                        mock(PaymentId.class),
                        mock(PaymentFailure.class),
                        Instant.parse("2026-09-01T00:20:00Z"),
                        mock(PaymentPolicyBundle.class),
                        IdempotencyKey.of("revoke-preconfirm-0001"),
                        CorrelationId.of(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        "PAYMENT_REJECTED"
                );

        assertThat(actual).isEqualTo(expected);
        verify(revocationService).revokeActiveChallenge(
                any(),
                any(),
                any(),
                any()
        );
        verify(finalizationService)
                .rejectAfterPreConfirmationRevocation(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void unstableRevocationPreventsRejectedTransition() {
        PaymentConfirmationRevocationService revocationService =
                mock(PaymentConfirmationRevocationService.class);
        PaymentFinalizationService finalizationService =
                mock(PaymentFinalizationService.class);

        PaymentConfirmationRevocationService.RevocationResult revocation =
                mock(PaymentConfirmationRevocationService.RevocationResult.class);
        when(revocation.stableForTerminalTransition())
                .thenReturn(false);
        when(revocationService.revokeActiveChallenge(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(revocation);

        PaymentPreConfirmationTerminationService service =
                new PaymentPreConfirmationTerminationService(
                        revocationService,
                        finalizationService
                );

        assertThatThrownBy(() ->
                service.reject(
                        mock(PaymentId.class),
                        mock(PaymentFailure.class),
                        Instant.parse("2026-09-01T00:21:00Z"),
                        mock(PaymentPolicyBundle.class),
                        IdempotencyKey.of("revoke-preconfirm-0002"),
                        CorrelationId.of(
                                "22222222-2222-4222-8222-222222222222"
                        ),
                        "PAYMENT_REJECTED"
                )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("stable revocation");

        verify(finalizationService, never())
                .rejectAfterPreConfirmationRevocation(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void technicalFailureFinalizesOnlyAfterStableRevocation() {
        PaymentConfirmationRevocationService revocationService =
                mock(PaymentConfirmationRevocationService.class);
        PaymentFinalizationService finalizationService =
                mock(PaymentFinalizationService.class);

        PaymentConfirmationRevocationService.RevocationResult revocation =
                mock(PaymentConfirmationRevocationService.RevocationResult.class);
        when(revocation.stableForTerminalTransition())
                .thenReturn(true);
        when(revocationService.revokeActiveChallenge(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(revocation);

        PaymentWorkflowResult expected =
                new PaymentWorkflowResult(
                        mock(PaymentId.class),
                        mock(
                                com.sixpay.payment.domain.model
                                        .PublicPaymentReference.class
                        ),
                        PaymentStatus.FAILED,
                        8L,
                        true
                );
        when(finalizationService.failAfterPreConfirmationRevocation(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(expected);

        PaymentPreConfirmationTerminationService service =
                new PaymentPreConfirmationTerminationService(
                        revocationService,
                        finalizationService
                );

        PaymentWorkflowResult actual =
                service.failWithoutFinancialEffect(
                        mock(PaymentId.class),
                        mock(PaymentFailure.class),
                        Instant.parse("2026-09-01T00:22:00Z"),
                        mock(PaymentPolicyBundle.class),
                        IdempotencyKey.of("revoke-preconfirm-0003"),
                        CorrelationId.of(
                                "33333333-3333-4333-8333-333333333333"
                        ),
                        "PAYMENT_FAILED"
                );

        assertThat(actual).isEqualTo(expected);
        verify(finalizationService)
                .failAfterPreConfirmationRevocation(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
}
