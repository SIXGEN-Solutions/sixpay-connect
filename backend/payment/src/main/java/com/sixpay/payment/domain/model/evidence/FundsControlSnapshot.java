package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class FundsControlSnapshot implements ValueObject {

    private final FundsVerificationReference verificationReference;
    private final FundsControlOutcome outcome;
    private final Money checkedAmount;
    private final String accountBindingFingerprint;
    private final List<FundsControlCheckEvidence> checks;
    private final Instant validUntil;
    private final EvidenceMetadata metadata;

    public FundsControlSnapshot(
            FundsVerificationReference verificationReference,
            FundsControlOutcome outcome,
            Money checkedAmount,
            String accountBindingFingerprint,
            List<FundsControlCheckEvidence> checks,
            Instant validUntil,
            EvidenceMetadata metadata
    ) {
        this.verificationReference = EvidenceValueObjectRules.requireNonNull(
                verificationReference,
                "Funds verification reference"
        );
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Funds-control outcome"
        );
        this.checkedAmount = EvidenceValueObjectRules.requireNonNull(
                checkedAmount,
                "Funds-control checked amount"
        );
        if (!checkedAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "Funds-control checked amount must be positive"
            );
        }
        this.accountBindingFingerprint =
                EvidenceValueObjectRules.requireAccountBindingFingerprint(
                        accountBindingFingerprint
                );
        this.checks = canonicalChecks(checks);
        this.validUntil = EvidenceValueObjectRules.requireNonNull(
                validUntil,
                "Funds-control validity instant"
        );
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Funds-control metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE) {
            throw new IllegalArgumentException(
                    "Funds-control source must be AMPLITUDE"
            );
        }

        EvidenceValueObjectRules.requireNotBefore(
                validUntil,
                metadata.acceptedAt(),
                "Funds-control validity must not precede acceptance"
        );
        validateOutcome();
    }

    private static List<FundsControlCheckEvidence> canonicalChecks(
            List<FundsControlCheckEvidence> values
    ) {
        EvidenceValueObjectRules.requireNonNull(
                values,
                "Funds-control checks"
        );
        if (values.isEmpty()
                || values.size() > FundsControlCheckType.values().length) {
            throw new IllegalArgumentException(
                    "Funds-control checks must contain 1 to "
                            + FundsControlCheckType.values().length
                            + " entries"
            );
        }

        Set<FundsControlCheckType> seen =
                EnumSet.noneOf(FundsControlCheckType.class);
        List<FundsControlCheckEvidence> canonical =
                new ArrayList<>(values.size());

        for (FundsControlCheckEvidence value : values) {
            FundsControlCheckEvidence validated =
                    EvidenceValueObjectRules.requireNonNull(
                            value,
                            "Funds-control check"
                    );
            if (!seen.add(validated.type())) {
                throw new IllegalArgumentException(
                        "Funds-control check types must be unique"
                );
            }
            canonical.add(validated);
        }

        canonical.sort(
                (left, right) ->
                        Integer.compare(
                                left.type().ordinal(),
                                right.type().ordinal()
                        )
        );
        return List.copyOf(canonical);
    }

    private void validateOutcome() {
        boolean hasFailure = checks.stream().anyMatch(
                check -> check.result() == EvidenceCheckResult.FAIL
        );
        boolean hasUnknown = checks.stream().anyMatch(
                check -> check.result() == EvidenceCheckResult.UNKNOWN
        );
        boolean allPass = checks.stream().allMatch(
                check -> check.result() == EvidenceCheckResult.PASS
        );

        switch (outcome) {
            case VERIFIED -> {
                if (!allPass) {
                    throw new IllegalArgumentException(
                            "Verified funds evidence must contain only PASS results"
                    );
                }
            }
            case REJECTED -> {
                if (!hasFailure) {
                    throw new IllegalArgumentException(
                            "Rejected funds evidence requires a FAIL result"
                    );
                }
            }
            case INDETERMINATE -> {
                if (hasFailure || !hasUnknown) {
                    throw new IllegalArgumentException(
                            "Indeterminate funds evidence requires UNKNOWN and no FAIL result"
                    );
                }
            }
        }
    }

    public FundsVerificationReference verificationReference() {
        return verificationReference;
    }

    public FundsControlOutcome outcome() {
        return outcome;
    }

    public Money checkedAmount() {
        return checkedAmount;
    }

    public String accountBindingFingerprint() {
        return accountBindingFingerprint;
    }

    public List<FundsControlCheckEvidence> checks() {
        return checks;
    }

    public Instant validUntil() {
        return validUntil;
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FundsControlSnapshot that)) {
            return false;
        }
        return verificationReference.equals(that.verificationReference)
                && outcome == that.outcome
                && checkedAmount.equals(that.checkedAmount)
                && accountBindingFingerprint.equals(
                        that.accountBindingFingerprint
                )
                && checks.equals(that.checks)
                && validUntil.equals(that.validUntil)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                verificationReference,
                outcome,
                checkedAmount,
                accountBindingFingerprint,
                checks,
                validUntil,
                metadata
        );
    }

    @Override
    public String toString() {
        return "FundsControlSnapshot[reference="
                + verificationReference
                + ", outcome=" + outcome
                + ", checkedAmount=" + checkedAmount
                + ", checkCount=" + checks.size()
                + ", validUntil=" + validUntil
                + ", metadata=" + metadata + "]";
    }
}
