package com.sixpay.payment.infrastructure.banking.amplitude.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AmplitudeFundsCheckResponse(
        String code,
        String verificationReference,
        String outcome,
        BigDecimal checkedAmount,
        String currency,
        String accountBindingFingerprint,
        Map<String, AmplitudeCheckResult> checks,
        Instant observedAt,
        Instant validUntil
) { }
