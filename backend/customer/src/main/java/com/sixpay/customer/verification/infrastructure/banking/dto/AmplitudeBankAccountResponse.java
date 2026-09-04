package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;
import java.util.List;

public record AmplitudeBankAccountResponse(
        String accountReference,
        String customerReference,
        String financialInstitutionCode,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        String status,
        List<String> restrictions,
        String source,
        Instant retrievedAt
) { }
