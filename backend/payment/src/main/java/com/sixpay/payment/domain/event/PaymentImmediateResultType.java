package com.sixpay.payment.domain.event;

public enum PaymentImmediateResultType {
    REJECTED,
    FAILED,
    PROCESSING,
    POSTED_PENDING_TFJ,
    REVERSAL_REQUIRED
}
