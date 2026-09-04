package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;
import java.util.List;

public record AmplitudeCustomerIdentityResponse(
        String customerReference,
        String customerNumber,
        String financialInstitutionCode,
        String niu,
        String legalName,
        String phoneNumber,
        String email,
        String kycStatus,
        List<AmplitudeKycFieldResponse> kycFields,
        Instant kycLastUpdatedAt,
        String source,
        Instant retrievedAt
) { }
