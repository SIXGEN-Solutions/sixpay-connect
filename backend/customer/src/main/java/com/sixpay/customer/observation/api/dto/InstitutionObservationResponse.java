package com.sixpay.customer.observation.api.dto;

import java.time.Instant;
import java.util.List;

public record InstitutionObservationResponse(
        String financialInstitutionCode,
        Instant firstObservedAt,
        Instant lastObservedAt,
        List<MaskedAccountReferenceResponse> accounts
) {
}
