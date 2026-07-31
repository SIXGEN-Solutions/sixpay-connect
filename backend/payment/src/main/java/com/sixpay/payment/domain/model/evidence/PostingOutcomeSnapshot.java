package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PostingOutcomeSnapshot implements ValueObject {

    private static final Set<EvidenceObservationChannel> ALLOWED_CHANNELS =
            Set.of(
                    EvidenceObservationChannel.DIRECT_RESPONSE,
                    EvidenceObservationChannel.IDEMPOTENCY_LOOKUP,
                    EvidenceObservationChannel.BANK_REFERENCE_LOOKUP
            );

    private final PostingInstructionId postingInstructionId;
    private final PostingIdempotencyKey postingCommandIdempotencyKey;
    private final PostingOutcome outcome;
    private final BankPostingReference bankPostingReference;
    private final PostingLegEvidence debitLeg;
    private final PostingLegEvidence cutCreditLeg;
    private final Money amount;
    private final LocalDate businessDate;
    private final FailureCode rejectionCode;
    private final PostingNextAction nextAction;
    private final EvidenceMetadata metadata;

    public PostingOutcomeSnapshot(
            PostingInstructionId postingInstructionId,
            PostingIdempotencyKey postingCommandIdempotencyKey,
            PostingOutcome outcome,
            BankPostingReference bankPostingReference,
            PostingLegEvidence debitLeg,
            PostingLegEvidence cutCreditLeg,
            Money amount,
            LocalDate businessDate,
            FailureCode rejectionCode,
            PostingNextAction nextAction,
            EvidenceMetadata metadata
    ) {
        this.postingInstructionId = EvidenceValueObjectRules.requireNonNull(
                postingInstructionId,
                "Posting instruction ID"
        );
        this.postingCommandIdempotencyKey =
                EvidenceValueObjectRules.requireNonNull(
                        postingCommandIdempotencyKey,
                        "Posting idempotency key"
                );
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Posting outcome"
        );
        this.bankPostingReference = bankPostingReference;
        this.debitLeg = EvidenceValueObjectRules.requireNonNull(
                debitLeg,
                "Debit leg evidence"
        );
        this.cutCreditLeg = EvidenceValueObjectRules.requireNonNull(
                cutCreditLeg,
                "CUT credit leg evidence"
        );
        this.amount = EvidenceValueObjectRules.requireNonNull(
                amount,
                "Posting amount"
        );
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Posting amount must be positive");
        }
        this.businessDate = businessDate;
        this.rejectionCode = rejectionCode;
        this.nextAction = EvidenceValueObjectRules.requireNonNull(
                nextAction,
                "Posting next action"
        );
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Posting evidence metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE
                || !ALLOWED_CHANNELS.contains(metadata.observationChannel())) {
            throw new IllegalArgumentException(
                    "Posting evidence source or channel is invalid"
            );
        }

        validateOutcome();
        validateReferenceConsistency();
    }

    private void validateOutcome() {
        switch (outcome) {
            case COMPLETED -> {
                requireLegStatus(
                        debitLeg,
                        PostingLegStatus.SUCCEEDED,
                        "Completed posting requires a successful debit leg"
                );
                requireLegStatus(
                        cutCreditLeg,
                        PostingLegStatus.SUCCEEDED,
                        "Completed posting requires a successful CUT credit leg"
                );
                requireBankPostingReference();
                requireNextAction(PostingNextAction.NONE);
                if (rejectionCode != null) {
                    throw new IllegalArgumentException(
                            "Completed posting must not have a rejection code"
                    );
                }
            }
            case REJECTED_NO_FINANCIAL_EFFECT -> {
                if (hasPossibleFinancialEffect(debitLeg)
                        || hasPossibleFinancialEffect(cutCreditLeg)) {
                    throw new IllegalArgumentException(
                            "Rejected posting must prove no financial effect"
                    );
                }
                if (rejectionCode == null) {
                    throw new IllegalArgumentException(
                            "Rejected posting requires a rejection code"
                    );
                }
                requireNextAction(PostingNextAction.NONE);
            }
            case DEBIT_CONFIRMED_CUT_CREDIT_PENDING -> {
                requireLegStatus(
                        debitLeg,
                        PostingLegStatus.SUCCEEDED,
                        "Debit-confirmed posting requires a successful debit leg"
                );
                if (cutCreditLeg.status() != PostingLegStatus.PENDING
                        && cutCreditLeg.status() != PostingLegStatus.UNKNOWN) {
                    throw new IllegalArgumentException(
                            "Debit-confirmed posting requires pending or unknown CUT credit"
                    );
                }
                requireBankPostingReference();
                if (!Set.of(
                        PostingNextAction.WAIT_FOR_CUT_CREDIT,
                        PostingNextAction.QUERY_OUTCOME,
                        PostingNextAction.OPEN_RECONCILIATION
                ).contains(nextAction)) {
                    throw new IllegalArgumentException(
                            "Debit-confirmed posting has an invalid next action"
                    );
                }
                if (rejectionCode != null) {
                    throw new IllegalArgumentException(
                            "Debit-confirmed posting must not have a rejection code"
                    );
                }
            }
            case REVERSAL_REQUIRED -> {
                if (debitLeg.status() != PostingLegStatus.SUCCEEDED
                        && cutCreditLeg.status() != PostingLegStatus.SUCCEEDED) {
                    throw new IllegalArgumentException(
                            "Reversal-required posting needs a confirmed financial effect"
                    );
                }
                requireBankPostingReference();
                requireNextAction(
                        PostingNextAction.REQUEST_EXPLICIT_REVERSAL
                );
            }
            case UNKNOWN -> {
                if (!Set.of(
                        PostingNextAction.QUERY_OUTCOME,
                        PostingNextAction.OPEN_RECONCILIATION
                ).contains(nextAction)) {
                    throw new IllegalArgumentException(
                            "Unknown posting requires lookup or reconciliation"
                    );
                }
                if (debitLeg.status() == PostingLegStatus.SUCCEEDED
                        && cutCreditLeg.status()
                        == PostingLegStatus.SUCCEEDED) {
                    throw new IllegalArgumentException(
                            "Unknown posting cannot have both legs confirmed"
                    );
                }
                if (rejectionCode != null) {
                    throw new IllegalArgumentException(
                            "Unknown posting must not claim a rejection"
                    );
                }
            }
        }
    }

    private void validateReferenceConsistency() {
        if (bankPostingReference == null) {
            return;
        }

        bankPostingReference.debitLegReference().ifPresent(
                reference -> debitLeg.bankEntryReferenceOptional().ifPresent(
                        entry -> {
                            if (!reference.equals(entry)) {
                                throw new IllegalArgumentException(
                                        "Debit leg references are inconsistent"
                                );
                            }
                        }
                )
        );

        bankPostingReference.cutCreditLegReference().ifPresent(
                reference -> cutCreditLeg.bankEntryReferenceOptional().ifPresent(
                        entry -> {
                            if (!reference.equals(entry)) {
                                throw new IllegalArgumentException(
                                        "CUT credit leg references are inconsistent"
                                );
                            }
                        }
                )
        );
    }

    private static boolean hasPossibleFinancialEffect(
            PostingLegEvidence leg
    ) {
        return Set.of(
                PostingLegStatus.SUCCEEDED,
                PostingLegStatus.PENDING,
                PostingLegStatus.UNKNOWN
        ).contains(leg.status());
    }

    private static void requireLegStatus(
            PostingLegEvidence leg,
            PostingLegStatus expected,
            String message
    ) {
        if (leg.status() != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireBankPostingReference() {
        if (bankPostingReference == null) {
            throw new IllegalArgumentException(
                    "Posting outcome requires a principal bank reference"
            );
        }
    }

    private void requireNextAction(PostingNextAction expected) {
        if (nextAction != expected) {
            throw new IllegalArgumentException(
                    "Posting outcome requires next action " + expected
            );
        }
    }

    public PostingInstructionId postingInstructionId() {
        return postingInstructionId;
    }

    public PostingIdempotencyKey postingCommandIdempotencyKey() {
        return postingCommandIdempotencyKey;
    }

    public PostingOutcome outcome() {
        return outcome;
    }

    public Optional<BankPostingReference> bankPostingReference() {
        return Optional.ofNullable(bankPostingReference);
    }

    public PostingLegEvidence debitLeg() {
        return debitLeg;
    }

    public PostingLegEvidence cutCreditLeg() {
        return cutCreditLeg;
    }

    public Money amount() {
        return amount;
    }

    public Optional<LocalDate> businessDate() {
        return Optional.ofNullable(businessDate);
    }

    public Optional<FailureCode> rejectionCode() {
        return Optional.ofNullable(rejectionCode);
    }

    public PostingNextAction nextAction() {
        return nextAction;
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PostingOutcomeSnapshot that)) {
            return false;
        }
        return postingInstructionId.equals(that.postingInstructionId)
                && postingCommandIdempotencyKey.equals(
                        that.postingCommandIdempotencyKey
                )
                && outcome == that.outcome
                && Objects.equals(
                        bankPostingReference,
                        that.bankPostingReference
                )
                && debitLeg.equals(that.debitLeg)
                && cutCreditLeg.equals(that.cutCreditLeg)
                && amount.equals(that.amount)
                && Objects.equals(businessDate, that.businessDate)
                && Objects.equals(rejectionCode, that.rejectionCode)
                && nextAction == that.nextAction
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                postingInstructionId,
                postingCommandIdempotencyKey,
                outcome,
                bankPostingReference,
                debitLeg,
                cutCreditLeg,
                amount,
                businessDate,
                rejectionCode,
                nextAction,
                metadata
        );
    }

    @Override
    public String toString() {
        return "PostingOutcomeSnapshot[instructionId="
                + postingInstructionId
                + ", outcome=" + outcome
                + ", bankReference=" + bankPostingReference
                + ", debitStatus=" + debitLeg.status()
                + ", cutCreditStatus=" + cutCreditLeg.status()
                + ", nextAction=" + nextAction
                + ", metadata=" + metadata + "]";
    }
}
