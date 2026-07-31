package com.sixpay.payment.domain.policy;

public record CurrentReversalEvidence(
        EvidenceIdentity identity,
        EvidenceAuthority authority,
        EvidenceConclusiveness conclusiveness
) {
}
