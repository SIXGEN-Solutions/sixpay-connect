package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.model.authorization.*;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import com.sixpay.payment.domain.policy.ReversalInstructionIdentity;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;

/**
 * Persistence-only representation of one complete immutable PaymentState.
 *
 * <p>This record is deliberately not a public integration contract. Its JSON
 * representation is stored input the {@code payments.state_payload} JSONB column
 * and may only be read through {@link PaymentPersistenceMapper}.</p>
 */
record PaymentStateDocument(
        int schemaVersion,
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        PaymentSource source,
        ExternalPaymentReference externalPaymentReference,
        ExternalSubscriptionReference externalSubscriptionReference,
        PaymentRequestIdentity requestIdentity,
        FinancialInstitutionCode financialInstitutionCode,
        DebtorAccountReference debtorAccountReference,
        Money requestedAmount,
        TreasuryAllocationIntent treasuryAllocationIntent,
        EvidenceFingerprint allocationIntentFingerprint,
        PaymentInitiationContext initiationContext,
        CustomerConfirmationEvidence customerConfirmationEvidence,
        ConfirmationChallenge confirmationChallenge,
        PaymentStatus status,
        SixpayAuthorizationDecisionSnapshot authorizationDecision,
        BankingVerificationSnapshot bankingVerificationEvidence,
        FundsControlSnapshot fundsControlEvidence,
        TreasuryAccountResolutionSnapshot treasuryResolutionEvidence,
        TreasuryAccountReference treasuryAccountReference,
        PostingInstructionIdentity postingInstruction,
        PostingOutcomeSnapshot postingOutcomeEvidence,
        BankPostingReference bankPostingReference,
        EndOfDayConfirmationSnapshot endOfDayConfirmationEvidence,
        ReversalInstructionIdentity reversalInstruction,
        ReversalAuthorizationEvidence reversalAuthorizationEvidence,
        ReversalSnapshot reversalEvidence,
        PaymentFailure failure,
        long businessVersion,
        Instant receivedAt,
        Instant updatedAt,
        Instant finalizedAt
) {

    static final int CURRENT_SCHEMA_VERSION = 5;

    static PaymentStateDocument from(PaymentState state) {
        return new PaymentStateDocument(
                CURRENT_SCHEMA_VERSION,
                state.paymentId(),
                state.publicPaymentReference(),
                state.source(),
                state.externalPaymentReference(),
                state.externalSubscriptionReference(),
                state.requestIdentity(),
                state.financialInstitutionCode(),
                state.debtorAccountReference(),
                state.requestedAmount(),
                state.treasuryAllocationIntent(),
                state.allocationIntentFingerprint(),
                state.initiationContext().orElse(null),
                state.customerConfirmationEvidence().orElse(null),
                state.confirmationChallenge().orElse(null),
                state.status(),
                state.authorizationDecision().orElse(null),
                state.bankingVerificationEvidence().orElse(null),
                state.fundsControlEvidence().orElse(null),
                state.treasuryResolutionEvidence().orElse(null),
                state.treasuryAccountReference().orElse(null),
                state.postingInstruction().orElse(null),
                state.postingOutcomeEvidence().orElse(null),
                state.bankPostingReference().orElse(null),
                state.endOfDayConfirmationEvidence().orElse(null),
                state.reversalInstruction().orElse(null),
                state.reversalAuthorizationEvidence().orElse(null),
                state.reversalEvidence().orElse(null),
                state.failure().orElse(null),
                state.businessVersion(),
                state.receivedAt(),
                state.updatedAt(),
                state.finalizedAt().orElse(null)
        );
    }

    private void validateSchemaCompatibility() {
        if (schemaVersion < 1
                || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new PaymentPersistenceException(
                    "Unsupported Payment state schema version: "
                            + schemaVersion
            );
        }

        if (schemaVersion == 1
                && (initiationContext != null
                || customerConfirmationEvidence != null
                || confirmationChallenge != null)) {
            throw new PaymentPersistenceException(
                    "Legacy Payment state payload must not contain "
                            + "initiation, confirmation evidence or challenge"
            );
        }

        if (schemaVersion == 2
                && confirmationChallenge != null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 must not contain "
                            + "confirmation challenge state"
            );
        }

        if (schemaVersion < 5
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.BANKING_VERIFICATION_PENDING
                && status != PaymentStatus.PENDING_CONFIRMATION
                && status != PaymentStatus.AUTHORIZATION_CHECKING
                && status != PaymentStatus.REJECTED
                && status != PaymentStatus.FAILED) {
            throw new PaymentPersistenceException(
                    "Payment state schema version "
                            + schemaVersion
                            + " predates the SIXPAY-local authorization gate "
                            + "and cannot reconstitute post-authorization state"
            );
        }

        if (schemaVersion >= 4
                && bankingVerificationEvidence != null
                && bankingVerificationEvidence.outcome()
                        == BankingVerificationOutcome.VERIFIED
                && (bankingVerificationEvidence
                                .customerReferenceOptional()
                                .isEmpty()
                        || bankingVerificationEvidence
                                .accountReferenceOptional()
                                .isEmpty())) {
            throw new PaymentPersistenceException(
                    "Payment state schema version "
                            + schemaVersion
                            + " requires canonical banking customer/account "
                            + "references for VERIFIED banking evidence"
            );
        }

        if (schemaVersion >= 2
                && status == PaymentStatus.PENDING_CONFIRMATION
                && initiationContext == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version "
                            + schemaVersion
                            + " requires initiation context for "
                            + "PENDING_CONFIRMATION"
            );
        }

        /*
         * Schema v2 predates ConfirmationChallenge. Preserve its historical
         * representation exactly: any state beyond RECEIVED or
         * PENDING_CONFIRMATION requires CustomerConfirmationEvidence.
         */
        if (schemaVersion == 2
                && initiationContext != null
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 requires confirmation "
                            + "evidence after confirmation"
            );
        }

        /*
         * Starting with schema v3, ConfirmationChallenge is persisted and a
         * verified challenge is a valid replacement for the legacy
         * CustomerConfirmationEvidence. Banking verification may legitimately
         * be pending before customer confirmation.
         *
         * REJECTED and FAILED are intentionally excluded because both can be
         * reached before successful OTP confirmation.
         */
        if (schemaVersion >= 3
                && initiationContext != null
                && requiresVerifiedConfirmation(status)
                && customerConfirmationEvidence == null
                && !hasVerifiedConfirmationChallenge()) {
            throw new PaymentPersistenceException(
                    "Payment state schema version "
                            + schemaVersion
                            + " requires verified confirmation after "
                            + "confirmation"
            );
        }
    }

    private static boolean requiresVerifiedConfirmation(
            PaymentStatus status
    ) {
        return switch (status) {
            case AUTHORIZATION_CHECKING,
                    FUNDS_CONTROL_PENDING,
                    TREASURY_ACCOUNT_RESOLUTION_PENDING,
                    APPROVED_FOR_POSTING,
                    POSTING_PENDING,
                    POSTING_OUTCOME_UNKNOWN,
                    DEBIT_CONFIRMED,
                    POSTED_PENDING_TFJ,
                    REVERSAL_REQUIRED,
                    REVERSAL_PENDING,
                    REVERSAL_OUTCOME_UNKNOWN,
                    TREASURY_INTEGRATED,
                    REVERSED -> true;
            case RECEIVED,
                    BANKING_VERIFICATION_PENDING,
                    PENDING_CONFIRMATION,
                    REJECTED,
                    FAILED -> false;
        };
    }

    private boolean hasVerifiedConfirmationChallenge() {
        return confirmationChallenge != null
                && confirmationChallenge.status()
                        == ConfirmationChallengeStatus.VERIFIED
                && confirmationChallenge.verifiedAt() != null;
    }

    PaymentState toState() {
        validateSchemaCompatibility();

        return PaymentState.builder()
                .paymentId(paymentId)
                .publicPaymentReference(publicPaymentReference)
                .source(source)
                .externalPaymentReference(externalPaymentReference)
                .externalSubscriptionReference(
                        externalSubscriptionReference
                )
                .requestIdentity(requestIdentity)
                .financialInstitutionCode(financialInstitutionCode)
                .debtorAccountReference(debtorAccountReference)
                .requestedAmount(requestedAmount)
                .treasuryAllocationIntent(treasuryAllocationIntent)
                .allocationIntentFingerprint(
                        allocationIntentFingerprint
                )
                .initiationContext(initiationContext)
                .customerConfirmationEvidence(
                        customerConfirmationEvidence
                )
                .confirmationChallenge(confirmationChallenge)
                .status(status)
                .authorizationDecision(authorizationDecision)
                .bankingVerificationEvidence(
                        bankingVerificationEvidence
                )
                .fundsControlEvidence(fundsControlEvidence)
                .treasuryResolutionEvidence(
                        treasuryResolutionEvidence
                )
                .treasuryAccountReference(treasuryAccountReference)
                .postingInstruction(postingInstruction)
                .postingOutcomeEvidence(postingOutcomeEvidence)
                .bankPostingReference(bankPostingReference)
                .endOfDayConfirmationEvidence(
                        endOfDayConfirmationEvidence
                )
                .reversalInstruction(reversalInstruction)
                .reversalAuthorizationEvidence(
                        reversalAuthorizationEvidence
                )
                .reversalEvidence(reversalEvidence)
                .failure(failure)
                .businessVersion(businessVersion)
                .receivedAt(receivedAt)
                .updatedAt(updatedAt)
                .finalizedAt(finalizedAt)
                .build();
    }
}
