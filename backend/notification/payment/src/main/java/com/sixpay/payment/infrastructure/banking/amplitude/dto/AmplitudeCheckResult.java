package com.sixpay.payment.infrastructure.banking.amplitude.dto;

import java.time.Instant;

public record AmplitudeCheckResult(
        String result,
        String reasonCode,
        Instant checkedAt
) { }
