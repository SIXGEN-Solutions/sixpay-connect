package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditIntegrityScheme;

import java.util.Objects;

public record IntegrityEvidenceView(
        AuditIntegrityScheme scheme,
        String value
) {
    public IntegrityEvidenceView {
        scheme = Objects.requireNonNull(scheme, "scheme is required");
        value = Objects.requireNonNull(value, "value is required");
    }
}
