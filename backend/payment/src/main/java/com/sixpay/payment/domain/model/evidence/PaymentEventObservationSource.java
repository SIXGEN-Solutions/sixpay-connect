package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum PaymentEventObservationSource implements ValueObject {
    DIRECT_RESPONSE,
    PAYMENT_REFERENCE_LOOKUP,
    IDEMPOTENCY_LOOKUP
}
