package com.sixpay.payment.infrastructure.banking.amplitude.reversal.dto;

import java.time.Instant;

public record AmplitudeReversalResponse(
        String code,
        String reversalInstructionId,
        String reversalIdempotencyKey,
        String originalBankPostingReference,
        String outcome,
        String reversalReference,
        String reversalEntryReference,
        String failureCode,
        Instant observedAt
) { }
