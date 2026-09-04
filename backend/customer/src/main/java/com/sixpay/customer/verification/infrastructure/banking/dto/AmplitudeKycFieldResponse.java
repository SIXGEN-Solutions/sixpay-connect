package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;

public record AmplitudeKycFieldResponse(
        String code,
        Object value,
        Boolean present,
        Boolean verified,
        Instant verifiedAt
) { }
