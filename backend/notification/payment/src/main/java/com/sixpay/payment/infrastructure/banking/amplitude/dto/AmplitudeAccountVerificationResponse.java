package com.sixpay.payment.infrastructure.banking.amplitude.dto;

import java.time.Instant;
import java.util.Map;

public record AmplitudeAccountVerificationResponse(
        String code,
        String verificationId,
        String outcome,
        String accountBindingFingerprint,
        Map<String, AmplitudeCheckResult> checks,
        Instant observedAt
) { }
