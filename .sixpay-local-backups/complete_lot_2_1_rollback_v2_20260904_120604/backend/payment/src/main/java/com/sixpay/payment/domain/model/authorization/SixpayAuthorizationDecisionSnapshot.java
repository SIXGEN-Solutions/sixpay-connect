package com.sixpay.payment.domain.model.authorization;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Durable result of the SIXPAY-local authorization gate executed after
 * successful customer confirmation and before Funds Control.
 *
 * <p>This snapshot is not provider evidence. It must never contain access
 * tokens, JWT material, OTP values or provider payloads.</p>
 */
public final class SixpayAuthorizationDecisionSnapshot implements ValueObject {

    private final SixpayAuthorizationDecision decision;
    private final List<SixpayAuthorizationCheckEvidence> checks;
    private final FailureCode rejectionCode;
    private final Instant decidedAt;

    public SixpayAuthorizationDecisionSnapshot(
            SixpayAuthorizationDecision decision,
            List<SixpayAuthorizationCheckEvidence> checks,
            FailureCode rejectionCode,
            Instant decidedAt
    ) {
        this.decision = Objects.requireNonNull(
                decision,
                "Authorization decision"
        );
        this.checks = canonicalChecks(checks);
        this.rejectionCode = rejectionCode;
        this.decidedAt = Objects.requireNonNull(
                decidedAt,
                "Authorization decision instant"
        );

        boolean hasFail = this.checks.stream().anyMatch(
                check -> check.result()
                        == SixpayAuthorizationCheckResult.FAIL
        );
        boolean allPass = this.checks.stream().allMatch(
                check -> check.result()
                        == SixpayAuthorizationCheckResult.PASS
        );

        if (decision == SixpayAuthorizationDecision.APPROVED) {
            if (!allPass) {
                throw new IllegalArgumentException(
                        "Approved SIXPAY authorization requires all checks PASS"
                );
            }
            if (rejectionCode != null) {
                throw new IllegalArgumentException(
                        "Approved SIXPAY authorization must not have rejection code"
                );
            }
        } else {
            if (!hasFail) {
                throw new IllegalArgumentException(
                        "Rejected SIXPAY authorization requires at least one failed check"
                );
            }
            if (rejectionCode == null) {
                throw new IllegalArgumentException(
                        "Rejected SIXPAY authorization requires rejection code"
                );
            }
        }
    }

    private static List<SixpayAuthorizationCheckEvidence> canonicalChecks(
            List<SixpayAuthorizationCheckEvidence> source
    ) {
        Objects.requireNonNull(source, "Authorization checks");

        Set<SixpayAuthorizationCheck> required =
                EnumSet.allOf(SixpayAuthorizationCheck.class);

        if (source.size() != required.size()) {
            throw new IllegalArgumentException(
                    "SIXPAY authorization requires exactly "
                            + required.size()
                            + " checks"
            );
        }

        Set<SixpayAuthorizationCheck> seen =
                EnumSet.noneOf(SixpayAuthorizationCheck.class);
        List<SixpayAuthorizationCheckEvidence> canonical =
                new ArrayList<>(source.size());

        for (SixpayAuthorizationCheckEvidence evidence : source) {
            SixpayAuthorizationCheckEvidence validated =
                    Objects.requireNonNull(
                            evidence,
                            "Authorization check evidence"
                    );
            if (!seen.add(validated.check())) {
                throw new IllegalArgumentException(
                        "SIXPAY authorization check types must be unique"
                );
            }
            canonical.add(validated);
        }

        if (!seen.equals(required)) {
            throw new IllegalArgumentException(
                    "SIXPAY authorization must evaluate every required check"
            );
        }

        canonical.sort(
                (left, right) -> Integer.compare(
                        left.check().ordinal(),
                        right.check().ordinal()
                )
        );
        return List.copyOf(canonical);
    }

    public SixpayAuthorizationDecision decision() {
        return decision;
    }

    public List<SixpayAuthorizationCheckEvidence> checks() {
        return checks;
    }

    public Optional<FailureCode> rejectionCode() {
        return Optional.ofNullable(rejectionCode);
    }

    public Instant decidedAt() {
        return decidedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SixpayAuthorizationDecisionSnapshot that)) {
            return false;
        }
        return decision == that.decision
                && checks.equals(that.checks)
                && Objects.equals(rejectionCode, that.rejectionCode)
                && decidedAt.equals(that.decidedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decision, checks, rejectionCode, decidedAt);
    }

    @Override
    public String toString() {
        return "SixpayAuthorizationDecisionSnapshot[decision="
                + decision
                + ", checkCount=" + checks.size()
                + ", rejectionCode=" + rejectionCode
                + ", decidedAt=" + decidedAt + "]";
    }
}
