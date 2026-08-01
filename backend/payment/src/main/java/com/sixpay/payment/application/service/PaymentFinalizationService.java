package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.domain.model.evidence.ReversalAuthorizationEvidence;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.ReversalInstructionIdentity;
import com.sixpay.payment.domain.policy.UniqueTfjMatchProof;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates posting outcomes, TFJ finality, reversal and terminal failures.
 *
 * <p>The class does not call a bank or TFJ source. It only applies evidence
 * already obtained by an inbound adapter or later gateway.</p>
 */
@Service
public class PaymentFinalizationService {

    private final PaymentMutationCoordinator coordinator;

    public PaymentFinalizationService(
            PaymentMutationCoordinator coordinator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    public PaymentWorkflowResult recordPostingOutcome(
            PaymentId paymentId,
            PostingOutcomeSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordPostingOutcome(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult resolvePostingOutcome(
            PaymentId paymentId,
            PostingOutcomeSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.resolvePostingOutcome(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult recordTfjConfirmation(
            PaymentId paymentId,
            EndOfDayConfirmationSnapshot evidence,
            UniqueTfjMatchProof matchProof,
            PaymentFailure reconciliationFailure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordMatchedEndOfDayConfirmation(
                                evidence,
                                matchProof,
                                reconciliationFailure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult authorizeReversal(
            PaymentId paymentId,
            ReversalInstructionIdentity instruction,
            ReversalAuthorizationEvidence authorization,
            Instant authorizedAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.authorizeReversal(
                                instruction,
                                authorization,
                                authorizedAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult recordReversalOutcome(
            PaymentId paymentId,
            ReversalSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordReversalOutcome(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult resolveReversalOutcome(
            PaymentId paymentId,
            ReversalSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.resolveReversalOutcome(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult reject(
            PaymentId paymentId,
            PaymentFailure rejection,
            Instant finalizedAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.reject(
                                rejection,
                                finalizedAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult recordRecoverableFailure(
            PaymentId paymentId,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordRecoverableFailure(
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult failWithoutFinancialEffect(
            PaymentId paymentId,
            PaymentFailure failure,
            Instant finalizedAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.failWithoutFinancialEffect(
                                failure,
                                finalizedAt,
                                policies
                        )
        );
    }
}
