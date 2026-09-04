package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;

public record AmplitudeVerificationCheckResponse(
        String type,
        String result,
        String reasonCode,
        Instant checkedAt
) { }
