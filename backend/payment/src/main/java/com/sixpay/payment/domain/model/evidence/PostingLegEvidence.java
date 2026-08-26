package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Optional;

public record PostingLegEvidence(
        PostingLegStatus status,
        String bankEntryReference,
        Instant effectiveAt,
        FailureCode failureCode
) implements ValueObject {

    public PostingLegEvidence {
        status = EvidenceValueObjectRules.requireNonNull(
                status,
                "Posting leg status"
        );
        if (bankEntryReference != null) {
            bankEntryReference =
                    EvidenceValueObjectRules
                            .requirePrintableAsciiNoWhitespace(
                                    bankEntryReference,
                                    1,
                                    128,
                                    "Posting leg bank entry reference"
                            );
        }
        if (status == PostingLegStatus.FAILED && failureCode == null) {
            throw new IllegalArgumentException(
                    "Failed posting leg requires a failure code"
            );
        }
        if (status != PostingLegStatus.FAILED && failureCode != null) {
            throw new IllegalArgumentException(
                    "Only a failed posting leg may contain a failure code"
            );
        }
    }

    public Optional<String> bankEntryReferenceOptional() {
        return Optional.ofNullable(bankEntryReference);
    }

    public Optional<Instant> effectiveAtOptional() {
        return Optional.ofNullable(effectiveAt);
    }

    public Optional<FailureCode> failureCodeOptional() {
        return Optional.ofNullable(failureCode);
    }
}
