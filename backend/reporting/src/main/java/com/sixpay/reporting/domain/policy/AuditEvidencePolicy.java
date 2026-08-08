package com.sixpay.reporting.domain.policy;

import com.sixpay.reporting.domain.model.AuditEvidenceOwner;

import java.util.Objects;

/**
 * Framework-free policy for evidence admitted into Reporting.
 */
public final class AuditEvidencePolicy {

    public void requireSupportedOwner(
            AuditEvidenceOwner owner
    ) {
        Objects.requireNonNull(
                owner,
                "evidence owner is required"
        );
    }
}
