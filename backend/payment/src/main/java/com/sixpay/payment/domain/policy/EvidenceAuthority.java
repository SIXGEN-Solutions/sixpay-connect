package com.sixpay.payment.domain.policy;

public enum EvidenceAuthority {
    DIRECT_RESPONSE(1),
    IDEMPOTENCY_LOOKUP(2),
    BANK_REFERENCE_LOOKUP(3),
    UNIQUE_TFJ_MATCH(4);

    private final int rank;

    EvidenceAuthority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
