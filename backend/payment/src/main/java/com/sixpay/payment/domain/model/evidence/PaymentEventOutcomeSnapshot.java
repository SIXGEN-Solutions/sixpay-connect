package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Atomic T0 Payment-event outcome returned authoritatively by Core Banking.
 *
 * <p>This model deliberately does not reconstruct the historical debit/CUT
 * leg model. The submitted Payment event is atomic from SIXPAY's point of
 * view.</p>
 */
public final class PaymentEventOutcomeSnapshot implements ValueObject {

    private final PostingInstructionId postingInstructionId;
    private final PostingIdempotencyKey postingCommandIdempotencyKey;
    private final PaymentEventOutcome outcome;
    private final List<FundsControlCheckEvidence> checks;
    private final BankPostingReference bankPostingReference;
    private final FailureCode reasonCode;
    private final LocalDate accountingDate;
    private final Instant observedAt;
    private final PaymentEventObservationSource observationSource;

    public PaymentEventOutcomeSnapshot(
            PostingInstructionId postingInstructionId,
            PostingIdempotencyKey postingCommandIdempotencyKey,
            PaymentEventOutcome outcome,
            List<FundsControlCheckEvidence> checks,
            BankPostingReference bankPostingReference,
            FailureCode reasonCode,
            LocalDate accountingDate,
            Instant observedAt,
            PaymentEventObservationSource observationSource
    ) {
        this.postingInstructionId = Objects.requireNonNull(
                postingInstructionId,
                "Posting instruction ID"
        );
        this.postingCommandIdempotencyKey = Objects.requireNonNull(
                postingCommandIdempotencyKey,
                "Posting idempotency key"
        );
        this.outcome = Objects.requireNonNull(
                outcome,
                "Payment event outcome"
        );
        this.checks = canonicalChecks(checks);
        this.bankPostingReference = bankPostingReference;
        this.reasonCode = reasonCode;
        this.accountingDate = Objects.requireNonNull(
                accountingDate,
                "Core Banking accounting date"
        );
        this.observedAt = Objects.requireNonNull(
                observedAt,
                "Payment event observation instant"
        );
        this.observationSource = Objects.requireNonNull(
                observationSource,
                "Payment event observation source"
        );

        validateOutcome();
    }

    private static List<FundsControlCheckEvidence> canonicalChecks(
            List<FundsControlCheckEvidence> values
    ) {
        Objects.requireNonNull(values, "Payment execution checks");

        if (values.size() != FundsControlCheckType.values().length) {
            throw new IllegalArgumentException(
                    "Payment event outcome requires exactly "
                            + FundsControlCheckType.values().length
                            + " execution checks"
            );
        }

        Set<FundsControlCheckType> seen =
                EnumSet.noneOf(FundsControlCheckType.class);
        List<FundsControlCheckEvidence> canonical =
                new ArrayList<>(values.size());

        for (FundsControlCheckEvidence value : values) {
            FundsControlCheckEvidence validated =
                    Objects.requireNonNull(
                            value,
                            "Payment execution check"
                    );
            if (!seen.add(validated.type())) {
                throw new IllegalArgumentException(
                        "Payment execution check types must be unique"
                );
            }
            canonical.add(validated);
        }

        if (seen.size() != FundsControlCheckType.values().length) {
            throw new IllegalArgumentException(
                    "Payment event outcome must contain every execution check"
            );
        }

        canonical.sort(
                (left, right) -> Integer.compare(
                        left.type().ordinal(),
                        right.type().ordinal()
                )
        );
        return List.copyOf(canonical);
    }

    private void validateOutcome() {
        boolean allPass = checks.stream().allMatch(
                check -> check.result() == EvidenceCheckResult.PASS
        );
        boolean hasFail = checks.stream().anyMatch(
                check -> check.result() == EvidenceCheckResult.FAIL
        );

        switch (outcome) {
            case COMPLETED -> {
                if (!allPass) {
                    throw new IllegalArgumentException(
                            "COMPLETED Payment event requires all checks PASS"
                    );
                }
                if (bankPostingReference == null) {
                    throw new IllegalArgumentException(
                            "COMPLETED Payment event requires bank reference"
                    );
                }
                if (reasonCode != null) {
                    throw new IllegalArgumentException(
                            "COMPLETED Payment event must not carry reasonCode"
                    );
                }
            }
            case REJECTED -> {
                if (!hasFail) {
                    throw new IllegalArgumentException(
                            "REJECTED Payment event requires at least one FAIL check"
                    );
                }
            }
            case UNKNOWN -> {
                // UNKNOWN deliberately makes no claim about financial effect.
            }
        }
    }

    public PostingInstructionId postingInstructionId() {
        return postingInstructionId;
    }

    public PostingIdempotencyKey postingCommandIdempotencyKey() {
        return postingCommandIdempotencyKey;
    }

    public PaymentEventOutcome outcome() {
        return outcome;
    }

    public List<FundsControlCheckEvidence> checks() {
        return checks;
    }

    public Optional<BankPostingReference> bankPostingReference() {
        return Optional.ofNullable(bankPostingReference);
    }

    public Optional<FailureCode> reasonCode() {
        return Optional.ofNullable(reasonCode);
    }

    public LocalDate accountingDate() {
        return accountingDate;
    }

    public Instant observedAt() {
        return observedAt;
    }

    public PaymentEventObservationSource observationSource() {
        return observationSource;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaymentEventOutcomeSnapshot that)) {
            return false;
        }
        return postingInstructionId.equals(that.postingInstructionId)
                && postingCommandIdempotencyKey.equals(
                        that.postingCommandIdempotencyKey
                )
                && outcome == that.outcome
                && checks.equals(that.checks)
                && Objects.equals(
                        bankPostingReference,
                        that.bankPostingReference
                )
                && Objects.equals(reasonCode, that.reasonCode)
                && accountingDate.equals(that.accountingDate)
                && observedAt.equals(that.observedAt)
                && observationSource == that.observationSource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                postingInstructionId,
                postingCommandIdempotencyKey,
                outcome,
                checks,
                bankPostingReference,
                reasonCode,
                accountingDate,
                observedAt,
                observationSource
        );
    }
}
