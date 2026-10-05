package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.command.ResendPaymentConfirmationCommand;
import com.sixpay.payment.application.command.VerifyPaymentConfirmationCommand;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyResult;
import com.sixpay.payment.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentConfirmationE2EBehaviorTest {
    private static final PublicPaymentReference REF =
            PublicPaymentReference.of("PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC");
    private static final CorrelationId CORRELATION =
            CorrelationId.of("40a11cb8-b32c-474e-bab2-e0b6f43138c8");
    private static final Instant NOW = Instant.parse("2026-09-16T20:00:00Z");

    @Test
    void verifyVerifiedTriggersAuthorizationRecheck() {
        Fixture f = fixture();
        PaymentConfirmationBankResult result =
                result("CHALLENGE-001", ConfirmationChallengeStatus.VERIFIED,
                        ConfirmationBusinessCode.OTP_VERIFIED, NOW.plusSeconds(10));
        when(f.idempotency.executeVerify(any(), eq(REF), any(), any(char[].class), any()))
                .thenReturn(PaymentConfirmationIdempotencyResult.executed(result));

        var view = f.service.verify(verifyCommand());

        assertThat(view.status()).isEqualTo(ConfirmationChallengeStatus.VERIFIED);
        verify(f.authorization).startAuthorization(
                eq(f.paymentId),
                argThat((ConfirmationChallenge c) ->
                        c.status() == ConfirmationChallengeStatus.VERIFIED
                                && c.challengeReference().equals(
                                        result.challengeReference()
                                )));
    }

    @Test
    void verifyExpiredNeverStartsAuthorization() {
        Fixture f = fixture();
        PaymentConfirmationBankResult result =
                result("CHALLENGE-001", ConfirmationChallengeStatus.EXPIRED,
                        ConfirmationBusinessCode.CHALLENGE_EXPIRED, null);
        when(f.idempotency.executeVerify(any(), eq(REF), any(), any(char[].class), any()))
                .thenReturn(PaymentConfirmationIdempotencyResult.executed(result));

        assertThat(f.service.verify(verifyCommand()).status())
                .isEqualTo(ConfirmationChallengeStatus.EXPIRED);
        verify(f.authorization, never()).startAuthorization(any(PaymentId.class), any(ConfirmationChallenge.class));
    }

    @Test
    void verifyLockedNeverStartsAuthorization() {
        Fixture f = fixture();
        PaymentConfirmationBankResult result =
                result("CHALLENGE-001", ConfirmationChallengeStatus.LOCKED,
                        ConfirmationBusinessCode.CHALLENGE_LOCKED, null);
        when(f.idempotency.executeVerify(any(), eq(REF), any(), any(char[].class), any()))
                .thenReturn(PaymentConfirmationIdempotencyResult.executed(result));

        assertThat(f.service.verify(verifyCommand()).status())
                .isEqualTo(ConfirmationChallengeStatus.LOCKED);
        verify(f.authorization, never()).startAuthorization(any(PaymentId.class), any(ConfirmationChallenge.class));
    }

    @Test
    void resendAttachesReplacementChallengeAndDoesNotAuthorize() {
        Fixture f = fixture();
        PaymentConfirmationBankResult replacement =
                result("CHALLENGE-002", ConfirmationChallengeStatus.ACTIVE,
                        ConfirmationBusinessCode.CHALLENGE_REPLACED, null);
        when(f.idempotency.executeReplace(
                eq(f.paymentId), eq(REF), eq(f.currentReference), any(), any(), any()))
                .thenReturn(PaymentConfirmationIdempotencyResult.executed(replacement));

        var view = f.service.resend(new ResendPaymentConfirmationCommand(
                REF, CORRELATION, IdempotencyKey.of("resend-key-0001")));

        assertThat(view.status()).isEqualTo(ConfirmationChallengeStatus.ACTIVE);
        verify(f.authorization).attachConfirmationChallenge(
                eq(f.paymentId),
                argThat(c -> c.challengeReference().equals(replacement.challengeReference())
                        && c.binding().equals(f.binding)),
                eq(NOW));
        verify(f.authorization, never()).startAuthorization(any(PaymentId.class), any(ConfirmationChallenge.class));
    }

    private static VerifyPaymentConfirmationCommand verifyCommand() {
        return new VerifyPaymentConfirmationCommand(
                REF, CORRELATION, IdempotencyKey.of("verify-key-0001"),
                "123456".toCharArray());
    }

    private static Fixture fixture() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        PaymentAuthorizationService authorization = mock(PaymentAuthorizationService.class);
        PaymentConfirmationGateway gateway = mock(PaymentConfirmationGateway.class);
        PaymentConfirmationIdempotencyPort idempotency =
                mock(PaymentConfirmationIdempotencyPort.class);
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        ConfirmationChallenge current = mock(ConfirmationChallenge.class);
        PaymentId paymentId = mock(PaymentId.class);
        ConfirmationChallengeReference currentReference =
                new ConfirmationChallengeReference("CHALLENGE-001");
        ConfirmationChallengeBinding binding = mock(ConfirmationChallengeBinding.class);

        when(lookup.findByPublicPaymentReference(REF)).thenReturn(Optional.of(payment));
        when(payment.id()).thenReturn(paymentId);
        when(payment.publicPaymentReference()).thenReturn(REF);
        when(payment.status()).thenReturn(PaymentStatus.PENDING_CONFIRMATION);
        when(payment.toState()).thenReturn(state);
        when(state.confirmationChallenge()).thenReturn(Optional.of(current));
        when(state.financialInstitutionCode()).thenReturn(FinancialInstitutionCode.of("BANK_CM"));
        when(state.updatedAt()).thenReturn(NOW.minusSeconds(1));
        when(current.challengeReference()).thenReturn(currentReference);
        when(current.binding()).thenReturn(binding);

        return new Fixture(
                new PaymentConfirmationService(lookup, authorization, gateway, idempotency),
                authorization, idempotency, paymentId, currentReference, binding);
    }

    private static PaymentConfirmationBankResult result(
            String reference,
            ConfirmationChallengeStatus status,
            ConfirmationBusinessCode code,
            Instant verifiedAt) {
        return new PaymentConfirmationBankResult(
                new ConfirmationChallengeReference(reference),
                status, code, null, NOW, NOW.plusSeconds(300), verifiedAt);
    }

    private record Fixture(
            PaymentConfirmationService service,
            PaymentAuthorizationService authorization,
            PaymentConfirmationIdempotencyPort idempotency,
            PaymentId paymentId,
            ConfirmationChallengeReference currentReference,
            ConfirmationChallengeBinding binding) {}
}
