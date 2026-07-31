package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import com.sixpay.payment.domain.policy.ReversalInstructionIdentity;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable complete state used for atomic Payment mutation and reconstitution.
 *
 * <p>Pending domain events are deliberately excluded from persisted state.</p>
 */
public final class PaymentState implements ValueObject {

    private final PaymentId paymentId;
    private final PublicPaymentReference publicPaymentReference;
    private final PaymentSource source;
    private final ExternalPaymentReference externalPaymentReference;
    private final ExternalSubscriptionReference externalSubscriptionReference;
    private final PaymentRequestIdentity requestIdentity;
    private final FinancialInstitutionCode financialInstitutionCode;
    private final DebtorAccountReference debtorAccountReference;
    private final Money requestedAmount;
    private final TreasuryAllocationIntent treasuryAllocationIntent;
    private final EvidenceFingerprint allocationIntentFingerprint;
    private final PaymentStatus status;
    private final AuthorizationEvidenceSnapshot authorizationEvidence;
    private final BankingVerificationSnapshot bankingVerificationEvidence;
    private final FundsControlSnapshot fundsControlEvidence;
    private final TreasuryAccountResolutionSnapshot treasuryResolutionEvidence;
    private final TreasuryAccountReference treasuryAccountReference;
    private final PostingInstructionIdentity postingInstruction;
    private final PostingOutcomeSnapshot postingOutcomeEvidence;
    private final BankPostingReference bankPostingReference;
    private final EndOfDayConfirmationSnapshot endOfDayConfirmationEvidence;
    private final ReversalInstructionIdentity reversalInstruction;
    private final ReversalAuthorizationEvidence reversalAuthorizationEvidence;
    private final ReversalSnapshot reversalEvidence;
    private final PaymentFailure failure;
    private final long businessVersion;
    private final Instant receivedAt;
    private final Instant updatedAt;
    private final Instant finalizedAt;

    private PaymentState(Builder builder) {
        paymentId = Objects.requireNonNull(builder.paymentId, "Payment ID");
        publicPaymentReference = Objects.requireNonNull(
                builder.publicPaymentReference,
                "Public Payment reference"
        );
        source = Objects.requireNonNull(builder.source, "Payment source");
        externalPaymentReference = Objects.requireNonNull(
                builder.externalPaymentReference,
                "External Payment reference"
        );
        externalSubscriptionReference = Objects.requireNonNull(
                builder.externalSubscriptionReference,
                "External Subscription reference"
        );
        requestIdentity = Objects.requireNonNull(
                builder.requestIdentity,
                "Request identity"
        );
        financialInstitutionCode = Objects.requireNonNull(
                builder.financialInstitutionCode,
                "Financial institution code"
        );
        debtorAccountReference = Objects.requireNonNull(
                builder.debtorAccountReference,
                "Debtor account reference"
        );
        requestedAmount = Objects.requireNonNull(
                builder.requestedAmount,
                "Requested amount"
        );
        treasuryAllocationIntent = Objects.requireNonNull(
                builder.treasuryAllocationIntent,
                "Treasury allocation intent"
        );
        allocationIntentFingerprint = Objects.requireNonNull(
                builder.allocationIntentFingerprint,
                "Allocation intent fingerprint"
        );
        status = Objects.requireNonNull(builder.status, "Payment status");
        authorizationEvidence = builder.authorizationEvidence;
        bankingVerificationEvidence = builder.bankingVerificationEvidence;
        fundsControlEvidence = builder.fundsControlEvidence;
        treasuryResolutionEvidence = builder.treasuryResolutionEvidence;
        treasuryAccountReference = builder.treasuryAccountReference;
        postingInstruction = builder.postingInstruction;
        postingOutcomeEvidence = builder.postingOutcomeEvidence;
        bankPostingReference = builder.bankPostingReference;
        endOfDayConfirmationEvidence =
                builder.endOfDayConfirmationEvidence;
        reversalInstruction = builder.reversalInstruction;
        reversalAuthorizationEvidence =
                builder.reversalAuthorizationEvidence;
        reversalEvidence = builder.reversalEvidence;
        failure = builder.failure;
        businessVersion = builder.businessVersion;
        receivedAt = Objects.requireNonNull(
                builder.receivedAt,
                "Received instant"
        );
        updatedAt = Objects.requireNonNull(
                builder.updatedAt,
                "Updated instant"
        );
        finalizedAt = builder.finalizedAt;

        validate();
    }

    private void validate() {
        if (source != PaymentSource.TRESOR_PAY) {
            throw new IllegalArgumentException(
                    "Payment source must be TRESOR_PAY"
            );
        }
        if (!financialInstitutionCode.equals(
                debtorAccountReference.financialInstitutionCode()
        )) {
            throw new IllegalArgumentException(
                    "Debtor-account institution must match Payment"
            );
        }
        if (!requestedAmount.isPositive()
                || !requestedAmount.equals(
                        treasuryAllocationIntent.totalAmount()
                )) {
            throw new IllegalArgumentException(
                    "Requested amount and allocation total are inconsistent"
            );
        }
        if (businessVersion <= 0) {
            throw new IllegalArgumentException(
                    "Business version must be positive"
            );
        }
        if (updatedAt.isBefore(receivedAt)) {
            throw new IllegalArgumentException(
                    "Updated instant must not precede receipt"
            );
        }
        if (status.isTerminal()) {
            if (finalizedAt == null
                    || finalizedAt.isBefore(receivedAt)
                    || finalizedAt.isBefore(updatedAt)) {
                throw new IllegalArgumentException(
                        "Terminal Payment requires a valid finalizedAt"
                );
            }
        } else if (finalizedAt != null) {
            throw new IllegalArgumentException(
                    "Non-terminal Payment must not have finalizedAt"
            );
        }

        if (postingInstruction != null) {
            if (!postingInstruction.amount().equals(requestedAmount)
                    || !postingInstruction
                            .accountBindingFingerprint()
                            .equals(
                                    debtorAccountReference
                                            .bindingFingerprint()
                            )) {
                throw new IllegalArgumentException(
                        "Posting instruction is not bound to Payment"
                );
            }
        }

        if (postingOutcomeEvidence != null) {
            if (postingInstruction == null
                    || !postingInstruction.instructionId().equals(
                            postingOutcomeEvidence.postingInstructionId()
                    )
                    || !postingInstruction.idempotencyKey().equals(
                            postingOutcomeEvidence
                                    .postingCommandIdempotencyKey()
                    )
                    || !requestedAmount.equals(
                            postingOutcomeEvidence.amount()
                    )) {
                throw new IllegalArgumentException(
                        "Posting evidence is not bound to the authorized instruction"
                );
            }
            postingOutcomeEvidence.bankPostingReference().ifPresent(
                    reference -> {
                        if (bankPostingReference != null
                                && !bankPostingReference.equals(reference)) {
                            throw new IllegalArgumentException(
                                    "Original bank posting reference is immutable"
                            );
                        }
                    }
            );
        }

        if (reversalInstruction != null) {
            if (reversalAuthorizationEvidence == null
                    || reversalEvidence == null
                    || !reversalInstruction.instructionId().equals(
                            reversalEvidence.reversalInstructionId()
                    )
                    || !reversalInstruction.idempotencyKey().equals(
                            reversalEvidence
                                    .reversalCommandIdempotencyKey()
                    )) {
                throw new IllegalArgumentException(
                        "Reversal state is structurally inconsistent"
                );
            }
        }

        validateLifecycleCoherence();
    }

    private void validateLifecycleCoherence() {
        switch (status) {
            case RECEIVED, AUTHORIZATION_CHECKING -> {
                // No favorable downstream evidence is required yet.
            }
            case BANKING_VERIFICATION_PENDING -> requireAuthorizationApproved();
            case FUNDS_CONTROL_PENDING -> {
                requireAuthorizationApproved();
                requireBankingVerified();
            }
            case TREASURY_ACCOUNT_RESOLUTION_PENDING -> {
                requireAuthorizationApproved();
                requireBankingVerified();
                requireFundsVerified();
            }
            case APPROVED_FOR_POSTING -> {
                requireFavorablePrePostingEvidence();
                requireTreasuryResolved();
            }
            case POSTING_PENDING -> {
                requireFavorablePrePostingEvidence();
                requireTreasuryResolved();
                requirePostingInstruction();
            }
            case POSTING_OUTCOME_UNKNOWN -> {
                requirePostingInstruction();
                requirePostingOutcome(PostingOutcome.UNKNOWN);
            }
            case DEBIT_CONFIRMED -> {
                requirePostingInstruction();
                requirePostingOutcome(
                        PostingOutcome
                                .DEBIT_CONFIRMED_CUT_CREDIT_PENDING
                );
                requireBankPostingReference();
            }
            case POSTED_PENDING_TFJ -> {
                requirePostingInstruction();
                requirePostingOutcome(PostingOutcome.COMPLETED);
                requireBankPostingReference();
            }
            case REVERSAL_REQUIRED -> requireBankPostingReference();
            case REVERSAL_PENDING -> {
                requireBankPostingReference();
                requireReversalInstruction();
            }
            case REVERSAL_OUTCOME_UNKNOWN -> {
                requireBankPostingReference();
                requireReversalInstruction();
                if (reversalEvidence.outcome().isEmpty()
                        || reversalEvidence.outcome()
                                .orElseThrow()
                                .outcome()
                                != ReversalOutcome.UNKNOWN) {
                    throw new IllegalArgumentException(
                            "Reversal-outcome-unknown state requires UNKNOWN evidence"
                    );
                }
            }
            case REJECTED -> {
                if (failure == null
                        || (failure.failureCategory()
                                != FailureCategory.BUSINESS_REJECTION
                        && failure.failureCategory()
                                != FailureCategory.SECURITY_REJECTION)) {
                    throw new IllegalArgumentException(
                            "REJECTED requires a business or security failure"
                    );
                }
            }
            case FAILED -> {
                if (failure == null
                        || failure.failureCategory()
                                != FailureCategory.TECHNICAL_FAILURE) {
                    throw new IllegalArgumentException(
                            "FAILED requires a technical failure"
                    );
                }
            }
            case TREASURY_INTEGRATED -> {
                requirePostingOutcome(PostingOutcome.COMPLETED);
                if (endOfDayConfirmationEvidence == null
                        || endOfDayConfirmationEvidence.tfjStatus()
                                != TfjStatus.INTEGRATED) {
                    throw new IllegalArgumentException(
                            "Treasury finality requires matched INTEGRATED evidence"
                    );
                }
            }
            case REVERSED -> {
                requireBankPostingReference();
                requireReversalInstruction();
                if (reversalEvidence.outcome().isEmpty()
                        || reversalEvidence.outcome()
                                .orElseThrow()
                                .outcome()
                                != ReversalOutcome.REVERSED) {
                    throw new IllegalArgumentException(
                            "REVERSED requires authoritative reversed evidence"
                    );
                }
            }
        }
    }

    private void requireAuthorizationApproved() {
        if (authorizationEvidence == null
                || authorizationEvidence.outcome()
                        != AuthorizationDecisionOutcome.APPROVED) {
            throw new IllegalArgumentException(
                    "Approved authorization evidence is required"
            );
        }
    }

    private void requireBankingVerified() {
        if (bankingVerificationEvidence == null
                || bankingVerificationEvidence.outcome()
                        != BankingVerificationOutcome.VERIFIED) {
            throw new IllegalArgumentException(
                    "Verified banking evidence is required"
            );
        }
    }

    private void requireFundsVerified() {
        if (fundsControlEvidence == null
                || fundsControlEvidence.outcome()
                        != FundsControlOutcome.VERIFIED) {
            throw new IllegalArgumentException(
                    "Verified funds evidence is required"
            );
        }
    }

    private void requireTreasuryResolved() {
        if (treasuryResolutionEvidence == null
                || treasuryResolutionEvidence.resolutionOutcome()
                        != TreasuryResolutionOutcome.RESOLVED
                || treasuryAccountReference == null) {
            throw new IllegalArgumentException(
                    "Resolved Treasury evidence is required"
            );
        }
    }

    private void requireFavorablePrePostingEvidence() {
        requireAuthorizationApproved();
        requireBankingVerified();
        requireFundsVerified();
    }

    private void requirePostingInstruction() {
        if (postingInstruction == null) {
            throw new IllegalArgumentException(
                    "Posting instruction is required"
            );
        }
    }

    private void requirePostingOutcome(PostingOutcome expected) {
        if (postingOutcomeEvidence == null
                || postingOutcomeEvidence.outcome() != expected) {
            throw new IllegalArgumentException(
                    "Posting outcome " + expected + " is required"
            );
        }
    }

    private void requireBankPostingReference() {
        if (bankPostingReference == null) {
            throw new IllegalArgumentException(
                    "Original bank posting reference is required"
            );
        }
    }

    private void requireReversalInstruction() {
        if (reversalInstruction == null
                || reversalAuthorizationEvidence == null
                || reversalEvidence == null) {
            throw new IllegalArgumentException(
                    "Authorized reversal instruction is required"
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public PublicPaymentReference publicPaymentReference() {
        return publicPaymentReference;
    }

    public PaymentSource source() {
        return source;
    }

    public ExternalPaymentReference externalPaymentReference() {
        return externalPaymentReference;
    }

    public ExternalSubscriptionReference externalSubscriptionReference() {
        return externalSubscriptionReference;
    }

    public PaymentRequestIdentity requestIdentity() {
        return requestIdentity;
    }

    public FinancialInstitutionCode financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public DebtorAccountReference debtorAccountReference() {
        return debtorAccountReference;
    }

    public Money requestedAmount() {
        return requestedAmount;
    }

    public TreasuryAllocationIntent treasuryAllocationIntent() {
        return treasuryAllocationIntent;
    }

    public EvidenceFingerprint allocationIntentFingerprint() {
        return allocationIntentFingerprint;
    }

    public PaymentStatus status() {
        return status;
    }

    public Optional<AuthorizationEvidenceSnapshot> authorizationEvidence() {
        return Optional.ofNullable(authorizationEvidence);
    }

    public Optional<BankingVerificationSnapshot>
            bankingVerificationEvidence() {
        return Optional.ofNullable(bankingVerificationEvidence);
    }

    public Optional<FundsControlSnapshot> fundsControlEvidence() {
        return Optional.ofNullable(fundsControlEvidence);
    }

    public Optional<TreasuryAccountResolutionSnapshot>
            treasuryResolutionEvidence() {
        return Optional.ofNullable(treasuryResolutionEvidence);
    }

    public Optional<TreasuryAccountReference> treasuryAccountReference() {
        return Optional.ofNullable(treasuryAccountReference);
    }

    public Optional<PostingInstructionIdentity> postingInstruction() {
        return Optional.ofNullable(postingInstruction);
    }

    public Optional<PostingOutcomeSnapshot> postingOutcomeEvidence() {
        return Optional.ofNullable(postingOutcomeEvidence);
    }

    public Optional<BankPostingReference> bankPostingReference() {
        return Optional.ofNullable(bankPostingReference);
    }

    public Optional<EndOfDayConfirmationSnapshot>
            endOfDayConfirmationEvidence() {
        return Optional.ofNullable(endOfDayConfirmationEvidence);
    }

    public Optional<ReversalInstructionIdentity> reversalInstruction() {
        return Optional.ofNullable(reversalInstruction);
    }

    public Optional<ReversalAuthorizationEvidence>
            reversalAuthorizationEvidence() {
        return Optional.ofNullable(reversalAuthorizationEvidence);
    }

    public Optional<ReversalSnapshot> reversalEvidence() {
        return Optional.ofNullable(reversalEvidence);
    }

    public Optional<PaymentFailure> failure() {
        return Optional.ofNullable(failure);
    }

    public long businessVersion() {
        return businessVersion;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> finalizedAt() {
        return Optional.ofNullable(finalizedAt);
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaymentState that)) {
            return false;
        }
        return businessVersion == that.businessVersion
                && paymentId.equals(that.paymentId)
                && publicPaymentReference.equals(
                        that.publicPaymentReference
                )
                && source == that.source
                && externalPaymentReference.equals(
                        that.externalPaymentReference
                )
                && externalSubscriptionReference.equals(
                        that.externalSubscriptionReference
                )
                && requestIdentity.equals(that.requestIdentity)
                && financialInstitutionCode.equals(
                        that.financialInstitutionCode
                )
                && debtorAccountReference.equals(
                        that.debtorAccountReference
                )
                && requestedAmount.equals(that.requestedAmount)
                && treasuryAllocationIntent.equals(
                        that.treasuryAllocationIntent
                )
                && allocationIntentFingerprint.equals(
                        that.allocationIntentFingerprint
                )
                && status == that.status
                && Objects.equals(
                        authorizationEvidence,
                        that.authorizationEvidence
                )
                && Objects.equals(
                        bankingVerificationEvidence,
                        that.bankingVerificationEvidence
                )
                && Objects.equals(
                        fundsControlEvidence,
                        that.fundsControlEvidence
                )
                && Objects.equals(
                        treasuryResolutionEvidence,
                        that.treasuryResolutionEvidence
                )
                && Objects.equals(
                        treasuryAccountReference,
                        that.treasuryAccountReference
                )
                && Objects.equals(
                        postingInstruction,
                        that.postingInstruction
                )
                && Objects.equals(
                        postingOutcomeEvidence,
                        that.postingOutcomeEvidence
                )
                && Objects.equals(
                        bankPostingReference,
                        that.bankPostingReference
                )
                && Objects.equals(
                        endOfDayConfirmationEvidence,
                        that.endOfDayConfirmationEvidence
                )
                && Objects.equals(
                        reversalInstruction,
                        that.reversalInstruction
                )
                && Objects.equals(
                        reversalAuthorizationEvidence,
                        that.reversalAuthorizationEvidence
                )
                && Objects.equals(
                        reversalEvidence,
                        that.reversalEvidence
                )
                && Objects.equals(failure, that.failure)
                && receivedAt.equals(that.receivedAt)
                && updatedAt.equals(that.updatedAt)
                && Objects.equals(finalizedAt, that.finalizedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                paymentId,
                publicPaymentReference,
                source,
                externalPaymentReference,
                externalSubscriptionReference,
                requestIdentity,
                financialInstitutionCode,
                debtorAccountReference,
                requestedAmount,
                treasuryAllocationIntent,
                allocationIntentFingerprint,
                status,
                authorizationEvidence,
                bankingVerificationEvidence,
                fundsControlEvidence,
                treasuryResolutionEvidence,
                treasuryAccountReference,
                postingInstruction,
                postingOutcomeEvidence,
                bankPostingReference,
                endOfDayConfirmationEvidence,
                reversalInstruction,
                reversalAuthorizationEvidence,
                reversalEvidence,
                failure,
                businessVersion,
                receivedAt,
                updatedAt,
                finalizedAt
        );
    }

    @Override
    public String toString() {
        return "PaymentState[paymentId=" + paymentId
                + ", publicReference=" + publicPaymentReference
                + ", status=" + status
                + ", businessVersion=" + businessVersion
                + ", receivedAt=" + receivedAt
                + ", updatedAt=" + updatedAt
                + ", finalizedAt=" + finalizedAt + "]";
    }

    public static final class Builder {

        private PaymentId paymentId;
        private PublicPaymentReference publicPaymentReference;
        private PaymentSource source;
        private ExternalPaymentReference externalPaymentReference;
        private ExternalSubscriptionReference externalSubscriptionReference;
        private PaymentRequestIdentity requestIdentity;
        private FinancialInstitutionCode financialInstitutionCode;
        private DebtorAccountReference debtorAccountReference;
        private Money requestedAmount;
        private TreasuryAllocationIntent treasuryAllocationIntent;
        private EvidenceFingerprint allocationIntentFingerprint;
        private PaymentStatus status;
        private AuthorizationEvidenceSnapshot authorizationEvidence;
        private BankingVerificationSnapshot bankingVerificationEvidence;
        private FundsControlSnapshot fundsControlEvidence;
        private TreasuryAccountResolutionSnapshot treasuryResolutionEvidence;
        private TreasuryAccountReference treasuryAccountReference;
        private PostingInstructionIdentity postingInstruction;
        private PostingOutcomeSnapshot postingOutcomeEvidence;
        private BankPostingReference bankPostingReference;
        private EndOfDayConfirmationSnapshot endOfDayConfirmationEvidence;
        private ReversalInstructionIdentity reversalInstruction;
        private ReversalAuthorizationEvidence reversalAuthorizationEvidence;
        private ReversalSnapshot reversalEvidence;
        private PaymentFailure failure;
        private long businessVersion;
        private Instant receivedAt;
        private Instant updatedAt;
        private Instant finalizedAt;

        private Builder() {
        }

        private Builder(PaymentState state) {
            paymentId = state.paymentId;
            publicPaymentReference = state.publicPaymentReference;
            source = state.source;
            externalPaymentReference = state.externalPaymentReference;
            externalSubscriptionReference =
                    state.externalSubscriptionReference;
            requestIdentity = state.requestIdentity;
            financialInstitutionCode = state.financialInstitutionCode;
            debtorAccountReference = state.debtorAccountReference;
            requestedAmount = state.requestedAmount;
            treasuryAllocationIntent = state.treasuryAllocationIntent;
            allocationIntentFingerprint =
                    state.allocationIntentFingerprint;
            status = state.status;
            authorizationEvidence = state.authorizationEvidence;
            bankingVerificationEvidence =
                    state.bankingVerificationEvidence;
            fundsControlEvidence = state.fundsControlEvidence;
            treasuryResolutionEvidence =
                    state.treasuryResolutionEvidence;
            treasuryAccountReference = state.treasuryAccountReference;
            postingInstruction = state.postingInstruction;
            postingOutcomeEvidence = state.postingOutcomeEvidence;
            bankPostingReference = state.bankPostingReference;
            endOfDayConfirmationEvidence =
                    state.endOfDayConfirmationEvidence;
            reversalInstruction = state.reversalInstruction;
            reversalAuthorizationEvidence =
                    state.reversalAuthorizationEvidence;
            reversalEvidence = state.reversalEvidence;
            failure = state.failure;
            businessVersion = state.businessVersion;
            receivedAt = state.receivedAt;
            updatedAt = state.updatedAt;
            finalizedAt = state.finalizedAt;
        }

        public Builder paymentId(PaymentId value) {
            paymentId = value;
            return this;
        }

        public Builder publicPaymentReference(
                PublicPaymentReference value
        ) {
            publicPaymentReference = value;
            return this;
        }

        public Builder source(PaymentSource value) {
            source = value;
            return this;
        }

        public Builder externalPaymentReference(
                ExternalPaymentReference value
        ) {
            externalPaymentReference = value;
            return this;
        }

        public Builder externalSubscriptionReference(
                ExternalSubscriptionReference value
        ) {
            externalSubscriptionReference = value;
            return this;
        }

        public Builder requestIdentity(PaymentRequestIdentity value) {
            requestIdentity = value;
            return this;
        }

        public Builder financialInstitutionCode(
                FinancialInstitutionCode value
        ) {
            financialInstitutionCode = value;
            return this;
        }

        public Builder debtorAccountReference(
                DebtorAccountReference value
        ) {
            debtorAccountReference = value;
            return this;
        }

        public Builder requestedAmount(Money value) {
            requestedAmount = value;
            return this;
        }

        public Builder treasuryAllocationIntent(
                TreasuryAllocationIntent value
        ) {
            treasuryAllocationIntent = value;
            return this;
        }

        public Builder allocationIntentFingerprint(
                EvidenceFingerprint value
        ) {
            allocationIntentFingerprint = value;
            return this;
        }

        public Builder status(PaymentStatus value) {
            status = value;
            return this;
        }

        public Builder authorizationEvidence(
                AuthorizationEvidenceSnapshot value
        ) {
            authorizationEvidence = value;
            return this;
        }

        public Builder bankingVerificationEvidence(
                BankingVerificationSnapshot value
        ) {
            bankingVerificationEvidence = value;
            return this;
        }

        public Builder fundsControlEvidence(
                FundsControlSnapshot value
        ) {
            fundsControlEvidence = value;
            return this;
        }

        public Builder treasuryResolutionEvidence(
                TreasuryAccountResolutionSnapshot value
        ) {
            treasuryResolutionEvidence = value;
            return this;
        }

        public Builder treasuryAccountReference(
                TreasuryAccountReference value
        ) {
            treasuryAccountReference = value;
            return this;
        }

        public Builder postingInstruction(
                PostingInstructionIdentity value
        ) {
            postingInstruction = value;
            return this;
        }

        public Builder postingOutcomeEvidence(
                PostingOutcomeSnapshot value
        ) {
            postingOutcomeEvidence = value;
            return this;
        }

        public Builder bankPostingReference(
                BankPostingReference value
        ) {
            bankPostingReference = value;
            return this;
        }

        public Builder endOfDayConfirmationEvidence(
                EndOfDayConfirmationSnapshot value
        ) {
            endOfDayConfirmationEvidence = value;
            return this;
        }

        public Builder reversalInstruction(
                ReversalInstructionIdentity value
        ) {
            reversalInstruction = value;
            return this;
        }

        public Builder reversalAuthorizationEvidence(
                ReversalAuthorizationEvidence value
        ) {
            reversalAuthorizationEvidence = value;
            return this;
        }

        public Builder reversalEvidence(ReversalSnapshot value) {
            reversalEvidence = value;
            return this;
        }

        public Builder failure(PaymentFailure value) {
            failure = value;
            return this;
        }

        public Builder businessVersion(long value) {
            businessVersion = value;
            return this;
        }

        public Builder receivedAt(Instant value) {
            receivedAt = value;
            return this;
        }

        public Builder updatedAt(Instant value) {
            updatedAt = value;
            return this;
        }

        public Builder finalizedAt(Instant value) {
            finalizedAt = value;
            return this;
        }

        public PaymentState build() {
            return new PaymentState(this);
        }
    }
}
