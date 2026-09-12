package com.sixpay.accounting.domain.model;

public enum TfjOperationalCategory {
    MATCHED,
    QUARANTINED_UNMATCHED,
    QUARANTINED_AMBIGUOUS,
    FAILED,
    FINALITY_PUBLICATION_PENDING,
    COMPLETED
}
