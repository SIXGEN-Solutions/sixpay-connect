package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.AuthorizationBindingResult;
import com.sixpay.payment.domain.model.evidence.AuthorizationBindingType;
import com.sixpay.payment.domain.model.evidence.AuthorizationDecisionOutcome;
import com.sixpay.payment.domain.model.evidence.AuthorizationEvidenceSnapshot;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;

import java.util.EnumMap;
import java.util.Objects;

public final class SixpayAuthorizationGate {

    public SixpayAuthorizationGateResult evaluate(PaymentState state) {
        return evaluate(state, null);
    }

    public SixpayAuthorizationGateResult evaluate(
            PaymentState state,
            PartnerAuthorizationView
                    partnerAuthorization
    ) {
        Objects.requireNonNull(state, "Payment state");

        EnumMap<
                AuthorizationControl,
                AuthorizationControlResult
        > results = new EnumMap<>(AuthorizationControl.class);

        for (AuthorizationControl control : AuthorizationControl.values()) {
            results.put(
                    control,
                    evaluateControl(
                            control,
                            state,
                            partnerAuthorization
                    )
            );
        }

        return new SixpayAuthorizationGateResult(results);
    }

    private AuthorizationControlResult evaluateControl(
            AuthorizationControl control,
            PaymentState state,
            PartnerAuthorizationView
                    partnerAuthorization
    ) {
        AuthorizationControlSource source =
                AuthorizationControlSourceMap.sourceFor(control);

        if (control == AuthorizationControl.PARTNER_AUTHORIZED) {
            if (partnerAuthorization == null) {
                return new AuthorizationControlResult(
                        control,
                        AuthorizationControlOutcome.UNRESOLVED,
                        "Partner authorization view is not available"
                );
            }
            if (!partnerAuthorization.active()) {
                return fail(
                        control,
                        "SIXPAY Partner is not ACTIVE"
                );
            }
            return new AuthorizationControlResult(
                    control,
                    AuthorizationControlOutcome.PASS,
                    "SIXPAY Partner is ACTIVE"
            );
        }

        if (control == AuthorizationControl.CLAIM_TYPE_AUTHORIZED) {
            if (state.initiationContext().isEmpty()) {
                return fail(
                        control,
                        "Payment initiation context is required"
                );
            }
            if (partnerAuthorization == null) {
                return new AuthorizationControlResult(
                        control,
                        AuthorizationControlOutcome.UNRESOLVED,
                        "Partner authorization view is not available"
                );
            }
            if (!partnerAuthorization.authorizes(
                    state.initiationContext()
                            .orElseThrow()
                            .claimType()
            )) {
                return fail(
                        control,
                        "Claim type is outside the Partner authorized perimeter"
                );
            }
            return new AuthorizationControlResult(
                    control,
                    AuthorizationControlOutcome.PASS,
                    "Claim type is authorized by Partner perimeter"
            );
        }

        if (control == AuthorizationControl.EXECUTION_DATE_VALID) {
            return evaluateExecutionDate(state);
        }

        if (control == AuthorizationControl.SUBSCRIPTION_AUTHORIZED) {
            return evaluateTrustedIntakeBinding(
                    control,
                    state,
                    AuthorizationBindingType.SUBSCRIPTION_REFERENCE,
                    "Trusted TRESOR PAY subscription authorization evidence"
            );
        }

        if (control == AuthorizationControl.APPLICATION_AUTHORIZED) {
            return evaluateTrustedIntakeBinding(
                    control,
                    state,
                    AuthorizationBindingType.CLIENT_APPLICATION,
                    "Trusted TRESOR PAY application authorization evidence"
            );
        }

        if (control == AuthorizationControl.REQUEST_DATA_CONSISTENT) {
            return evaluateRequestDataConsistency(state);
        }

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

        return new AuthorizationControlResult(
                control,
                AuthorizationControlOutcome.UNRESOLVED,
                source.evidence()
        );
    }

    private AuthorizationControlResult evaluateTrustedIntakeBinding(
            AuthorizationControl control,
            PaymentState state,
            AuthorizationBindingType bindingType,
            String successReason
    ) {
        if (state.status() != PaymentStatus.AUTHORIZATION_CHECKING) {
            return fail(control, "Payment must be AUTHORIZATION_CHECKING");
        }

        AuthorizationEvidenceSnapshot evidence =
                state.authorizationEvidence().orElse(null);

        if (evidence == null) {
            return new AuthorizationControlResult(
                    control,
                    AuthorizationControlOutcome.UNRESOLVED,
                    "Trusted TRESOR PAY authorization evidence is not durably available"
            );
        }

        if (evidence.outcome() != AuthorizationDecisionOutcome.APPROVED) {
            return fail(
                    control,
                    "Trusted TRESOR PAY authorization evidence is not APPROVED"
            );
        }

        AuthorizationBindingResult bindingResult =
                evidence.bindingResults().stream()
                        .filter(binding -> binding.type() == bindingType)
                        .map(binding -> binding.result())
                        .findFirst()
                        .orElse(AuthorizationBindingResult.NOT_EVALUATED);

        if (bindingResult == AuthorizationBindingResult.MISMATCH) {
            return fail(
                    control,
                    "Trusted TRESOR PAY authorization binding does not match"
            );
        }

        if (bindingResult != AuthorizationBindingResult.MATCH) {
            return new AuthorizationControlResult(
                    control,
                    AuthorizationControlOutcome.UNRESOLVED,
                    "Trusted TRESOR PAY authorization binding was not evaluated"
            );
        }

        return new AuthorizationControlResult(
                control,
                AuthorizationControlOutcome.PASS,
                successReason
        );
    }

    private AuthorizationControlResult evaluateExecutionDate(
            PaymentState state
    ) {
        AuthorizationControl control =
                AuthorizationControl.EXECUTION_DATE_VALID;

        if (state.status() != PaymentStatus.AUTHORIZATION_CHECKING) {
            return fail(control, "Payment must be AUTHORIZATION_CHECKING");
        }

        if (state.initiationContext().isEmpty()) {
            return fail(control, "Payment initiation context is required");
        }

        return new AuthorizationControlResult(
                control,
                AuthorizationControlOutcome.PASS,
                "Direct payment has no SIXPAY banking-calendar restriction"
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
