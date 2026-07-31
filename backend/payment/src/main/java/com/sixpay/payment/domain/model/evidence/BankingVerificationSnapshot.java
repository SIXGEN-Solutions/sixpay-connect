package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BankingVerificationSnapshot implements ValueObject {

    private final BankingVerificationId verificationId;
    private final BankingVerificationOutcome outcome;
    private final String accountBindingFingerprint;
    private final List<BankingVerificationCheckEvidence> checks;
    private final EvidenceMetadata metadata;

    public BankingVerificationSnapshot(
            BankingVerificationId verificationId,
            BankingVerificationOutcome outcome,
            String accountBindingFingerprint,
            List<BankingVerificationCheckEvidence> checks,
            EvidenceMetadata metadata
    ) {
        this.verificationId = EvidenceValueObjectRules.requireNonNull(
                verificationId,
                "Banking verification ID"
        );
        this.outcome = EvidenceValueObjectRules.requireNonNull(
                outcome,
                "Banking verification outcome"
        );
        this.accountBindingFingerprint =
                EvidenceValueObjectRules.requireAccountBindingFingerprint(
                        accountBindingFingerprint
                );
        this.checks = canonicalChecks(checks);
        this.metadata = EvidenceValueObjectRules.requireNonNull(
                metadata,
                "Banking verification metadata"
        );

        if (metadata.sourceSystem() != ExternalSystem.AMPLITUDE) {
            throw new IllegalArgumentException(
                    "Banking verification source must be AMPLITUDE"
            );
        }
        validateOutcome();
    }

    private static List<BankingVerificationCheckEvidence> canonicalChecks(
            List<BankingVerificationCheckEvidence> values
    ) {
        EvidenceValueObjectRules.requireNonNull(
                values,
                "Banking verification checks"
        );
        if (values.isEmpty()
                || values.size()
                > BankingVerificationCheckType.values().length) {
            throw new IllegalArgumentException(
                    "Banking verification checks must contain 1 to "
                            + BankingVerificationCheckType.values().length
                            + " entries"
            );
        }

        Set<BankingVerificationCheckType> seen =
                EnumSet.noneOf(BankingVerificationCheckType.class);
        List<BankingVerificationCheckEvidence> canonical =
                new ArrayList<>(values.size());

        for (BankingVerificationCheckEvidence value : values) {
            BankingVerificationCheckEvidence validated =
                    EvidenceValueObjectRules.requireNonNull(
                            value,
                            "Banking verification check"
                    );
            if (!seen.add(validated.type())) {
                throw new IllegalArgumentException(
                        "Banking verification check types must be unique"
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
                            "Verified banking evidence must contain only PASS results"
                    );
                }
            }
            case REJECTED -> {
                if (!hasFailure) {
                    throw new IllegalArgumentException(
                            "Rejected banking evidence requires a FAIL result"
                    );
                }
            }
            case INDETERMINATE -> {
                if (hasFailure || !hasUnknown) {
                    throw new IllegalArgumentException(
                            "Indeterminate banking evidence requires UNKNOWN and no FAIL result"
                    );
                }
            }
        }
    }

    public BankingVerificationId verificationId() {
        return verificationId;
    }

    public BankingVerificationOutcome outcome() {
        return outcome;
    }

    public String accountBindingFingerprint() {
        return accountBindingFingerprint;
    }

    public List<BankingVerificationCheckEvidence> checks() {
        return checks;
    }

    public EvidenceMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BankingVerificationSnapshot that)) {
            return false;
        }
        return verificationId.equals(that.verificationId)
                && outcome == that.outcome
                && accountBindingFingerprint.equals(
                        that.accountBindingFingerprint
                )
                && checks.equals(that.checks)
                && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                verificationId,
                outcome,
                accountBindingFingerprint,
                checks,
                metadata
        );
    }

    @Override
    public String toString() {
        return "BankingVerificationSnapshot[verificationId="
                + verificationId
                + ", outcome=" + outcome
                + ", checkCount=" + checks.size()
                + ", metadata=" + metadata + "]";
    }
}
