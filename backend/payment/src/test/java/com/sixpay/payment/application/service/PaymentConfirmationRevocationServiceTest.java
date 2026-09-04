package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

class PaymentConfirmationRevocationServiceTest {

    @Test
    void activeChallengeUsesIdempotentRevokeAndPersistsRevokedSnapshot() {
        Payment payment = pendingPayment(
                activeChallenge()
        );

        PaymentLookupPort lookup =
                mock(PaymentLookupPort.class);
        when(lookup.findById(payment.id()))
                .thenReturn(Optional.of(payment));

        PaymentConfirmationGateway gateway =
                mock(PaymentConfirmationGateway.class);
        PaymentConfirmationIdempotencyPort idempotency =
                mock(PaymentConfirmationIdempotencyPort.class);
        PaymentAuthorizationService authorization =
                mock(PaymentAuthorizationService.class);

        PaymentConfirmationBankResult revoked =
                revokedBankResult(
                        payment.toState()
                                .confirmationChallenge()
                                .orElseThrow()
                );

        when(idempotency.executeRevoke(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                PaymentConfirmationIdempotencyResult.executed(
                        revoked
                )
        );

        PaymentConfirmationRevocationService service =
                new PaymentConfirmationRevocationService(
                        lookup,
                        authorization,
                        gateway,
                        idempotency
                );

        PaymentConfirmationRevocationService.RevocationResult result =
                service.revokeActiveChallenge(
                        payment.id(),
                        IdempotencyKey.of("revoke-key-1001"),
                        CorrelationId.of(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        "PAYMENT_REJECTED"
                );

        assertThat(result.stableForTerminalTransition()).isTrue();
        assertThat(result.bankOperationPerformed()).isTrue();
        assertThat(result.replayed()).isFalse();

        verify(idempotency).executeRevoke(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(authorization).attachConfirmationChallenge(
                any(),
                any(ConfirmationChallenge.class),
                any()
        );
    }

    @Test
    void alreadyRevokedChallengeSkipsBankAndIsStableForContinuation() {
        ConfirmationChallenge revokedChallenge =
                challengeWithStatus(
                        ConfirmationChallengeStatus.REVOKED,
                        ConfirmationBusinessCode.CHALLENGE_REVOKED
                );
        Payment payment =
                pendingPayment(revokedChallenge);

        PaymentLookupPort lookup =
                mock(PaymentLookupPort.class);
        when(lookup.findById(payment.id()))
                .thenReturn(Optional.of(payment));

        PaymentConfirmationGateway gateway =
                mock(PaymentConfirmationGateway.class);
        PaymentConfirmationIdempotencyPort idempotency =
                mock(PaymentConfirmationIdempotencyPort.class);
        PaymentAuthorizationService authorization =
                mock(PaymentAuthorizationService.class);

        PaymentConfirmationRevocationService service =
                new PaymentConfirmationRevocationService(
                        lookup,
                        authorization,
                        gateway,
                        idempotency
                );

        PaymentConfirmationRevocationService.RevocationResult result =
                service.revokeActiveChallenge(
                        payment.id(),
                        IdempotencyKey.of("revoke-key-1002"),
                        CorrelationId.of(
                                "22222222-2222-4222-8222-222222222222"
                        ),
                        "PAYMENT_REJECTED"
                );

        assertThat(result.stableForTerminalTransition()).isTrue();
        assertThat(result.bankOperationPerformed()).isFalse();

        verify(idempotency, never()).executeRevoke(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(authorization, never())
                .attachConfirmationChallenge(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void verifiedChallengeIsNeverRevoked() {
        Payment payment =
                pendingPayment(
                        challengeWithStatus(
                                ConfirmationChallengeStatus.VERIFIED,
                                ConfirmationBusinessCode.OTP_VERIFIED
                        )
                );

        PaymentLookupPort lookup =
                mock(PaymentLookupPort.class);
        when(lookup.findById(payment.id()))
                .thenReturn(Optional.of(payment));

        PaymentConfirmationIdempotencyPort idempotency =
                mock(PaymentConfirmationIdempotencyPort.class);

        PaymentConfirmationRevocationService service =
                new PaymentConfirmationRevocationService(
                        lookup,
                        mock(PaymentAuthorizationService.class),
                        mock(PaymentConfirmationGateway.class),
                        idempotency
                );

        assertThatThrownBy(() ->
                service.revokeActiveChallenge(
                        payment.id(),
                        IdempotencyKey.of("revoke-key-1003"),
                        CorrelationId.of(
                                "33333333-3333-4333-8333-333333333333"
                        ),
                        "PAYMENT_REJECTED"
                )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Only ACTIVE");

        verify(idempotency, never()).executeRevoke(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void nonRevokedBankResultIsRejectedAndNotPersisted() {
        Payment payment =
                pendingPayment(activeChallenge());

        PaymentLookupPort lookup =
                mock(PaymentLookupPort.class);
        when(lookup.findById(payment.id()))
                .thenReturn(Optional.of(payment));

        PaymentAuthorizationService authorization =
                mock(PaymentAuthorizationService.class);
        PaymentConfirmationIdempotencyPort idempotency =
                mock(PaymentConfirmationIdempotencyPort.class);

        ConfirmationChallenge current =
                payment.toState()
                        .confirmationChallenge()
                        .orElseThrow();

        PaymentConfirmationBankResult active =
                new PaymentConfirmationBankResult(
                        current.challengeReference(),
                        ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.CHALLENGE_ACTIVE,
                        current.deliveryChannel(),
                        current.sentAt(),
                        current.expiresAt(),
                        null
                );

        when(idempotency.executeRevoke(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                PaymentConfirmationIdempotencyResult.executed(
                        active
                )
        );

        PaymentConfirmationRevocationService service =
                new PaymentConfirmationRevocationService(
                        lookup,
                        authorization,
                        mock(PaymentConfirmationGateway.class),
                        idempotency
                );

        assertThatThrownBy(() ->
                service.revokeActiveChallenge(
                        payment.id(),
                        IdempotencyKey.of("revoke-key-1004"),
                        CorrelationId.of(
                                "44444444-4444-4444-8444-444444444444"
                        ),
                        "PAYMENT_FAILED"
                )
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("requires REVOKED");

        verify(authorization, never())
                .attachConfirmationChallenge(
                        any(),
                        any(),
                        any()
                );
    }

    private static Payment pendingPayment(
            ConfirmationChallenge challenge
    ) {
        Payment payment =
                mock(Payment.class, RETURNS_DEEP_STUBS);

        PaymentId paymentId =
                mock(PaymentId.class);

        when(payment.id()).thenReturn(paymentId);
        when(payment.status())
                .thenReturn(PaymentStatus.PENDING_CONFIRMATION);
        when(payment.toState().confirmationChallenge())
                .thenReturn(Optional.ofNullable(challenge));
        when(payment.toState().financialInstitutionCode())
                .thenReturn(
                        mock(
                                com.sixpay.payment.domain.model
                                        .FinancialInstitutionCode.class
                        )
                );
        when(payment.toState().updatedAt())
                .thenReturn(
                        java.time.Instant.parse(
                                "2026-08-31T20:00:00Z"
                        )
                );
        when(payment.publicPaymentReference())
                .thenReturn(
                        mock(
                                com.sixpay.payment.domain.model
                                        .PublicPaymentReference.class
                        )
                );
        return payment;
    }

    private static ConfirmationChallenge activeChallenge() {
        return challengeWithStatus(
                ConfirmationChallengeStatus.ACTIVE,
                ConfirmationBusinessCode.CHALLENGE_ACTIVE
        );
    }

    private static ConfirmationChallenge challengeWithStatus(
            ConfirmationChallengeStatus status,
            ConfirmationBusinessCode businessCode
    ) {
        ConfirmationChallenge challenge =
                mock(ConfirmationChallenge.class);

        when(challenge.status()).thenReturn(status);
        when(challenge.active())
                .thenReturn(
                        status == ConfirmationChallengeStatus.ACTIVE
                );
        when(challenge.challengeReference())
                .thenReturn(
                        mock(
                                com.sixpay.payment.domain.model
                                        .ConfirmationChallengeReference.class
                        )
                );
        when(challenge.binding())
                .thenReturn(
                        mock(
                                com.sixpay.payment.domain.model
                                        .ConfirmationChallengeBinding.class
                        )
                );
        when(challenge.businessCode())
                .thenReturn(businessCode);
        return challenge;
    }

    private static PaymentConfirmationBankResult revokedBankResult(
            ConfirmationChallenge current
    ) {
        return new PaymentConfirmationBankResult(
                current.challengeReference(),
                ConfirmationChallengeStatus.REVOKED,
                ConfirmationBusinessCode.CHALLENGE_REVOKED,
                null,
                java.time.Instant.parse(
                        "2026-08-31T20:05:00Z"
                ),
                null,
                null
        );
    }
}
