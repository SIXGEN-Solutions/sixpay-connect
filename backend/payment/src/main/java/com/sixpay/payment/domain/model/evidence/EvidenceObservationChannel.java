package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum EvidenceObservationChannel implements ValueObject {
    LOCAL_VALIDATION,
    DIRECT_RESPONSE,
    IDEMPOTENCY_LOOKUP,
    BANK_REFERENCE_LOOKUP,
    ASYNC_CALLBACK,
    SCHEDULED_LOOKUP,
    PROTECTED_CONFIGURATION_RESOLUTION
}
