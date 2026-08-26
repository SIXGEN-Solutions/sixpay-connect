package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import com.sixpay.payment.domain.policy.ReversalInstructionIdentity;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;

/**
 * Persistence-only representation of one complete immutable PaymentState.
 *
 * <p>This record is deliberately not a public integration contract. Its JSON
 * representation is stored in the {@code payments.state_payload} JSONB column
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
        PaymentStatus status,
        AuthorizationEvidenceSnapshot authorizationEvidence,
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

    static final int CURRENT_SCHEMA_VERSION = 2;

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
                state.status(),
                state.authorizationEvidence().orElse(null),
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
                || customerConfirmationEvidence != null)) {
            throw new PaymentPersistenceException(
                    "Legacy Payment state payload must not contain "
                            + "initiation context or confirmation evidence"
            );
        }

        if (schemaVersion == CURRENT_SCHEMA_VERSION
                && status == PaymentStatus.PENDING_CONFIRMATION
                && initiationContext == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 requires "
                            + "initiation context for PENDING_CONFIRMATION"
            );
        }

        if (schemaVersion == CURRENT_SCHEMA_VERSION
                && initiationContext != null
                && status != PaymentStatus.RECEIVED
                && status != PaymentStatus.PENDING_CONFIRMATION
                && customerConfirmationEvidence == null) {
            throw new PaymentPersistenceException(
                    "Payment state schema version 2 requires "
                            + "confirmation evidence after confirmation"
            );
        }
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
                .status(status)
                .authorizationEvidence(authorizationEvidence)
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
