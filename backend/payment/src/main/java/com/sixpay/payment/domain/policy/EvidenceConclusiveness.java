package com.sixpay.payment.domain.policy;

public enum EvidenceConclusiveness {
    INDETERMINATE(1),
    PARTIAL(2),
    CONCLUSIVE(3),
    FINAL(4);

    private final int rank;

    EvidenceConclusiveness(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
