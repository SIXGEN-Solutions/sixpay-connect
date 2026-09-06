package com.sixpay.payment.domain.event;

public enum PostingLookupMode {
    IDEMPOTENCY_KEY,
    BANK_REFERENCE,
    PAYMENT_REFERENCE_AND_IDEMPOTENCY_KEY
}
