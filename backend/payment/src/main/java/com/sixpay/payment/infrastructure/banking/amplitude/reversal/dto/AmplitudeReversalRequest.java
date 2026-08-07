package com.sixpay.payment.infrastructure.banking.amplitude.reversal.dto;

import java.time.Instant;

public record AmplitudeReversalRequest(
        String paymentId,
        String bankPostingReference,
        String authorizationType,
        String authorizationReference,
        String requestedBySubject,
        String reasonCode,
        Instant authorizedAt,
        Instant requestedAt,
        String financialInstitutionCode
) { }
