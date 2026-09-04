package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;

import java.util.EnumMap;
import java.util.Objects;

public final class SixpayAuthorizationGate {

    public SixpayAuthorizationGateResult evaluate(PaymentState state) {
        Objects.requireNonNull(state, "Payment state");

        EnumMap<
                AuthorizationControl,
                AuthorizationControlResult
        > results = new EnumMap<>(AuthorizationControl.class);

        for (AuthorizationControl control : AuthorizationControl.values()) {
            results.put(control, evaluateControl(control, state));
        }

        return new SixpayAuthorizationGateResult(results);
    }

    private AuthorizationControlResult evaluateControl(
            AuthorizationControl control,
            PaymentState state
    ) {
        AuthorizationControlSource source =
                AuthorizationControlSourceMap.sourceFor(control);

        if (source.implementationStatus()
                == AuthorizationControlSource
                .ImplementationStatus
                .REQUIRES_RUNTIME_SOURCE) {
            return new AuthorizationControlResult(
                    control,
                    AuthorizationControlOutcome.UNRESOLVED,
                    source.evidence()
            );
        }

        if (control == AuthorizationControl.REQUEST_DATA_CONSISTENT) {
            return evaluateRequestDataConsistency(state);
        }

        return new AuthorizationControlResult(
                control,
                AuthorizationControlOutcome.UNRESOLVED,
                source.evidence()
        );
    }

    private AuthorizationControlResult evaluateRequestDataConsistency(
            PaymentState state
    ) {
        AuthorizationControl control =
                AuthorizationControl.REQUEST_DATA_CONSISTENT;

        if (state.status() != PaymentStatus.AUTHORIZATION_CHECKING) {
            return fail(control, "Payment must be AUTHORIZATION_CHECKING");
        }

        if (state.initiationContext().isEmpty()) {
            return fail(control, "Payment initiation context is required");
        }

        if (!state.requestedAmount().equals(
                state.treasuryAllocationIntent().totalAmount()
        )) {
            return fail(
                    control,
                    "Requested amount and Treasury allocation total differ"
            );
        }

        if (!state.financialInstitutionCode().equals(
                state.debtorAccountReference()
                        .financialInstitutionCode()
        )) {
            return fail(
                    control,
                    "Debtor account institution differs from Payment institution"
            );
        }

        if (state.bankingVerificationEvidence().isEmpty()) {
            return fail(
                    control,
                    "Verified banking evidence is required"
            );
        }

        if (state.bankingVerificationEvidence()
                .orElseThrow()
                .outcome() != BankingVerificationOutcome.VERIFIED) {
            return fail(
                    control,
                    "Banking verification must be VERIFIED"
            );
        }

        boolean verifiedChallenge =
                state.confirmationChallenge()
                        .filter(challenge ->
                                challenge.status()
                                        == ConfirmationChallengeStatus.VERIFIED
                        )
                        .filter(challenge ->
                                challenge.optionalVerifiedAt().isPresent()
                        )
                        .isPresent();

        boolean legacyConfirmationEvidence =
                state.customerConfirmationEvidence().isPresent();

        if (!verifiedChallenge && !legacyConfirmationEvidence) {
            return fail(
                    control,
                    "Verified customer confirmation is required"
            );
        }

        return new AuthorizationControlResult(
                control,
                AuthorizationControlOutcome.PASS,
                "Payment-owned request, banking and confirmation facts are coherent"
        );
    }

    private static AuthorizationControlResult fail(
            AuthorizationControl control,
            String reason
    ) {
        return new AuthorizationControlResult(
                control,
                AuthorizationControlOutcome.FAIL,
                reason
        );
    }
}
