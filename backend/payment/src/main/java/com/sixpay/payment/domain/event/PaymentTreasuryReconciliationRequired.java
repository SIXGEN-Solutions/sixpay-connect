package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A matched adverse TFJ result requires manual reconciliation or reversal review.
 */
public record PaymentTreasuryReconciliationRequired(
        PaymentEventMetadata metadata,
        TfjConfirmationId confirmationId,
        String principalPostingReference,
        LocalDate businessDate,
        FailureCode failureCode,
        TfjRecoveryAction recoveryAction,
        Instant requiredAt
) implements PaymentDomainEvent {

    public PaymentTreasuryReconciliationRequired {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        confirmationId = Objects.requireNonNull(confirmationId, "confirmationId");
        principalPostingReference = Objects.requireNonNull(principalPostingReference, "principalPostingReference");
        principalPostingReference = principalPostingReference.strip();
        if (principalPostingReference.isEmpty() || principalPostingReference.length() > 256) {
            throw new IllegalArgumentException("principalPostingReference has an invalid length");
        }
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        failureCode = Objects.requireNonNull(failureCode, "failureCode");
        recoveryAction = Objects.requireNonNull(recoveryAction, "recoveryAction");
        requiredAt = Objects.requireNonNull(requiredAt, "requiredAt");
        if (recoveryAction == TfjRecoveryAction.REVERSAL_REQUIRED) {
            throw new IllegalArgumentException(
                    "Treasury reconciliation event cannot request direct reversal"
            );
        }
    }
}
