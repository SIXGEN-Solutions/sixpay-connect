package com.sixpay.payment.domain.policy;

public record CurrentPostingEvidence(
        EvidenceIdentity identity,
        EvidenceAuthority authority,
        EvidenceConclusiveness conclusiveness
) {
}
