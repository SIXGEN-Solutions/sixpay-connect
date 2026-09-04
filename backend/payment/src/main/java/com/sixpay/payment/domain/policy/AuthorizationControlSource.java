package com.sixpay.payment.domain.policy;

import java.util.Objects;

/**
 * Source-of-truth mapping for one AUTHORIZATION_CHECKING control.
 */
public record AuthorizationControlSource(
        AuthorizationControl control,
        AuthorizationSourceKind sourceKind,
        String owner,
        String evidence,
        ImplementationStatus implementationStatus
) {

    public AuthorizationControlSource {
        control = Objects.requireNonNull(control, "Control");
        sourceKind = Objects.requireNonNull(sourceKind, "Source kind");
        owner = requireText(owner, "Owner");
        evidence = requireText(evidence, "Evidence");
        implementationStatus = Objects.requireNonNull(
                implementationStatus,
                "Implementation status"
        );
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }
        return value;
    }

    public enum ImplementationStatus {
        READY,
        REQUIRES_RUNTIME_SOURCE
    }
}
