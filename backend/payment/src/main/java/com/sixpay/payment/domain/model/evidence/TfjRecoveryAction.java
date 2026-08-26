package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum TfjRecoveryAction implements ValueObject {
    MANUAL_RECONCILIATION,
    REVERSAL_REVIEW,
    REVERSAL_REQUIRED
}
