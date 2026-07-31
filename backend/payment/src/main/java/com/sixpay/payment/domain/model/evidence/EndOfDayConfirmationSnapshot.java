package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EndOfDayConfirmationSnapshot
        implements ValueObject {

    private static final Set<EvidenceObservationChannel> ALLOWED_CHANNELS =
            Set.of(
                    EvidenceObservationChannel.ASYNC_CALLBACK,
                    EvidenceObservationChannel.SCHEDULED_LOOKUP
            );

    private final TfjConfirmationId confirmationId;
    private final FinancialInstitutionCode financialInstitutionCode;
    private final LocalDate businessDate;
    private final PublicPaymentReference publicPaymentReference;
    private final String principalBankPostingReference;
    private final String tfjBatchReference;
    private final TfjStatus tfjStatus;
    private final TfjFailureEvidence failureEvidence;
    private final Instant confirmedAt;
    private final Instant matchedAt;
    private final EvidenceMetadata metadata;

    public EndOfDayConfirmationSnapshot(
            TfjConfirmationId confirmationId,
            FinancialInstitutionCode financialInstitutionCode,
            LocalDate businessDate,
            PublicPaymentReference publicPaymentReference,
            String principalBankPostingReference,
            String tfjBatchReference,
            TfjStatus tfjStatus,
            TfjFailureEvidence failureEvidence,
            Instant confirmedAt,
            Instant matchedAt,
            EvidenceMetadata metadata
    ) {
        this.confirmationId = EvidenceValueObjectRules.requireNonNull(
                confirmationId,
                "TFJ confirmation ID"
        );
        this.financialInstitutionCode =
                EvidenceValueObjectRules.requireNonNull(
                        financialInstitutionCode,
                        "Financial institution code"
                );
        this.businessDate = EvidenceValueObjectRules.requireNonNull(
                businessDate,
                "TFJ business date"
        );
        this.publicPaymentReference =
                EvidenceValueObjectRules.requireNonNull(
                        publicPaymentReference,
                        "Public Payment reference"
                );
        this.principalBankPostingReference =
                EvidenceValueObjectRules.requirePrintableAsciiNoWhitespace(
                        principalBankPostingReference,
                        1,
                        128,
                        "Principal bank posting reference"
                );
        this.tfjBatchReference = tfjBatchReference == null
                ? null
                : EvidenceValueObjectRules
                        .requirePrintableAsciiNoWhitespace(
                                tfjBatchReference,
                                1,
                                128,
                                "TFJ batch reference"
                        );
        this.tfjStatus = EvidenceValueObjectRules.requireNonNull(
                tfjStatus,
                "TFJ status"
        );
        this.failureEvidence = failureEvidence;
        this.confirmedAt = EvidenceValueObjectRules.requireNonNull(
                confirmedAt,
                "TFJ confirmation instant"
        );
        this.matchedAt = EvidenceValueObjectRules.requireNonNull(
                matchedAt,
                "TFJ match instant"
        );
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "TFJ evidence metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE
                || !ALLOWED_CHANNELS.contains(
                        metadata.observationChannel()
                )) {
            throw new IllegalArgumentException(
                    "TFJ evidence source or channel is invalid"
            );
        }

        EvidenceValueObjectRules.requireNotBefore(
                matchedAt,
                confirmedAt,
                "TFJ match instant must not precede confirmation"
        );

        if (tfjStatus == TfjStatus.INTEGRATED
                && failureEvidence != null) {
            throw new IllegalArgumentException(
                    "Integrated TFJ evidence must not contain failure evidence"
            );
        }

        if (tfjStatus == TfjStatus.FAILED
                && failureEvidence == null) {
            throw new IllegalArgumentException(
                    "Failed TFJ evidence requires failure evidence"
            );
        }
    }

    public TfjConfirmationId confirmationId() {
        return confirmationId;
    }

    public FinancialInstitutionCode financialInstitutionCode() {
        return financialInstitutionCode;
    }

    public LocalDate businessDate() {
        return businessDate;
    }

    public PublicPaymentReference publicPaymentReference() {
        return publicPaymentReference;
    }

    public String principalBankPostingReference() {
        return principalBankPostingReference;
    }

    public Optional<String> tfjBatchReference() {
        return Optional.ofNullable(tfjBatchReference);
    }

    public TfjStatus tfjStatus() {
        return tfjStatus;
    }

    public Optional<TfjFailureEvidence> failureEvidence() {
        return Optional.ofNullable(failureEvidence);
    }

    public Instant confirmedAt() {
        return confirmedAt;
    }

    public Instant matchedAt() {
        return matchedAt;
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EndOfDayConfirmationSnapshot that)) {
            return false;
        }
        return confirmationId.equals(that.confirmationId)
                && financialInstitutionCode.equals(
                        that.financialInstitutionCode
                )
                && businessDate.equals(that.businessDate)
                && publicPaymentReference.equals(
                        that.publicPaymentReference
                )
                && principalBankPostingReference.equals(
                        that.principalBankPostingReference
                )
                && Objects.equals(
                        tfjBatchReference,
                        that.tfjBatchReference
                )
                && tfjStatus == that.tfjStatus
                && Objects.equals(
                        failureEvidence,
                        that.failureEvidence
                )
                && confirmedAt.equals(that.confirmedAt)
                && matchedAt.equals(that.matchedAt)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                confirmationId,
                financialInstitutionCode,
                businessDate,
                publicPaymentReference,
                principalBankPostingReference,
                tfjBatchReference,
                tfjStatus,
                failureEvidence,
                confirmedAt,
                matchedAt,
                metadata
        );
    }

    @Override
    public String toString() {
        return "EndOfDayConfirmationSnapshot[confirmationId="
                + confirmationId
                + ", bank=" + financialInstitutionCode
                + ", businessDate=" + businessDate
                + ", paymentReference=" + publicPaymentReference
                + ", postingReference="
                + principalBankPostingReference
                + ", status=" + tfjStatus
                + ", matchedAt=" + matchedAt + "]";
    }
}
