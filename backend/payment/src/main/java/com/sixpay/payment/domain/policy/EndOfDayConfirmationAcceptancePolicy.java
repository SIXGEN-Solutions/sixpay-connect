package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;
import com.sixpay.payment.domain.model.evidence.TfjRecoveryAction;
import com.sixpay.payment.domain.model.evidence.TfjStatus;

import java.time.Instant;
import java.util.Objects;

public final class EndOfDayConfirmationAcceptancePolicy {

    public EndOfDayInterpretation decide(
            PaymentTfjContext context,
            EndOfDayConfirmationSnapshot evidence,
            UniqueTfjMatchProof matchProof,
            Instant decisionAt,
            TfjPolicyProfile profile,
            PaymentFailure failure
    ) {
        Objects.requireNonNull(context, "TFJ context");
        Objects.requireNonNull(evidence, "TFJ evidence");
        Objects.requireNonNull(matchProof, "Unique TFJ match proof");
        Objects.requireNonNull(decisionAt, "Decision instant");
        Objects.requireNonNull(profile, "TFJ profile");

        if (!profile.metadata().isEffectiveAt(decisionAt)
                || !profile.acceptedFinalStatuses().contains(
                        evidence.tfjStatus()
                )) {
            return new EndOfDayInterpretation(
                    EndOfDayDecision.QUARANTINE_CONFLICT,
                    failure
            );
        }

        if (!matchProof.confirmationId().equals(
                evidence.confirmationId()
        ) || !matchProof.isConclusive()) {
            return new EndOfDayInterpretation(
                    EndOfDayDecision.QUARANTINE_CONFLICT,
                    failure
            );
        }

        if (!context.financialInstitutionCode().equals(
                evidence.financialInstitutionCode()
        ) || !context.businessDate().equals(evidence.businessDate())
                || !context.publicPaymentReference().equals(
                        evidence.publicPaymentReference()
                )
                || !context.principalBankPostingReference().equals(
                        evidence.principalBankPostingReference()
                )) {
            return new EndOfDayInterpretation(
                    EndOfDayDecision.QUARANTINE_CONFLICT,
                    failure
            );
        }

        if (evidence.tfjStatus() == TfjStatus.INTEGRATED) {
            return new EndOfDayInterpretation(
                    EndOfDayDecision.TREASURY_INTEGRATED,
                    null
            );
        }

        TfjRecoveryAction action = evidence.failureEvidence()
                .orElseThrow()
                .recoveryAction();

        if (profile.reversalRequiredActions().contains(action)) {
            return new EndOfDayInterpretation(
                    EndOfDayDecision.REVERSAL_REQUIRED,
                    failure
            );
        }

        return new EndOfDayInterpretation(
                EndOfDayDecision.MANUAL_RECONCILIATION,
                failure
        );
    }
}
