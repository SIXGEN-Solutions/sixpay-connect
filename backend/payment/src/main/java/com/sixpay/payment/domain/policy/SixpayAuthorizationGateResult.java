package com.sixpay.payment.domain.policy;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

public final class SixpayAuthorizationGateResult {

    public enum Outcome {
        APPROVED,
        REJECTED,
        INCOMPLETE
    }

    private final Map<AuthorizationControl, AuthorizationControlResult> results;
    private final Outcome outcome;

    public SixpayAuthorizationGateResult(
            Map<AuthorizationControl, AuthorizationControlResult> results
    ) {
        Objects.requireNonNull(results, "Results");

        EnumMap<AuthorizationControl, AuthorizationControlResult> copy =
                new EnumMap<>(AuthorizationControl.class);
        copy.putAll(results);

        if (!copy.keySet().equals(
                EnumSet.allOf(AuthorizationControl.class)
        )) {
            throw new IllegalArgumentException(
                    "Gate result must contain exactly the six authorization controls"
            );
        }

        for (Map.Entry<
                AuthorizationControl,
                AuthorizationControlResult
        > entry : copy.entrySet()) {
            if (entry.getValue() == null
                    || entry.getKey() != entry.getValue().control()) {
                throw new IllegalArgumentException(
                        "Authorization control result key/value mismatch"
                );
            }
        }

        this.results = Map.copyOf(copy);
        this.outcome = deriveOutcome(copy);
    }

    private static Outcome deriveOutcome(
            Map<AuthorizationControl, AuthorizationControlResult> results
    ) {
        if (results.values().stream().anyMatch(
                result -> result.outcome()
                        == AuthorizationControlOutcome.FAIL
        )) {
            return Outcome.REJECTED;
        }

        if (results.values().stream().anyMatch(
                result -> result.outcome()
                        == AuthorizationControlOutcome.UNRESOLVED
        )) {
            return Outcome.INCOMPLETE;
        }

        return Outcome.APPROVED;
    }

    public Map<AuthorizationControl, AuthorizationControlResult> results() {
        return results;
    }

    public AuthorizationControlResult resultFor(
            AuthorizationControl control
    ) {
        AuthorizationControlResult result = results.get(control);
        if (result == null) {
            throw new IllegalArgumentException(
                    "No result for control " + control
            );
        }
        return result;
    }

    public Outcome outcome() {
        return outcome;
    }

    public boolean approved() {
        return outcome == Outcome.APPROVED;
    }

    public boolean rejected() {
        return outcome == Outcome.REJECTED;
    }

    public boolean incomplete() {
        return outcome == Outcome.INCOMPLETE;
    }
}
