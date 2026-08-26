package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AmplitudePostingResponse(
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
        LocalDate businessDate,
        String rejectionCode,
        String nextAction,
        Instant observedAt
) { }
