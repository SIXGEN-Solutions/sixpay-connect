package com.sixpay.payment.infrastructure.banking.amplitude.status.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AmplitudePostingStatusResponse(
        String code,
        String postingInstructionId,
        String postingIdempotencyKey,
        String outcome,
        String principalPostingReference,
        String debitLegReference,
        String debitLegStatus,
        Instant debitEffectiveAt,
        String debitFailureCode,
        String cutCreditLegReference,
        String cutCreditLegStatus,
        Instant cutCreditEffectiveAt,
        String cutCreditFailureCode,
        BigDecimal amount,
        String currency,
        LocalDate businessDate,
        String rejectionCode,
        String nextAction,
        Instant observedAt
) { }
