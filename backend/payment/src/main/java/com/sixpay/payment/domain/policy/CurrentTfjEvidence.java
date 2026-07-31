package com.sixpay.payment.domain.policy;

public record CurrentTfjEvidence(
        EvidenceIdentity identity,
        EvidenceAuthority authority,
        EvidenceConclusiveness conclusiveness
) {
}
