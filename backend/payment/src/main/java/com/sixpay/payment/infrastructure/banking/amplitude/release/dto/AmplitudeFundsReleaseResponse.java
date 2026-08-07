package com.sixpay.payment.infrastructure.banking.amplitude.release.dto;

import java.time.Instant;

public record AmplitudeFundsReleaseResponse(
        String code,
        String outcome,
        String reservationReference,
        String releaseReference,
        String failureCode,
        Instant observedAt
) { }
