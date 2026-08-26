package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.math.BigDecimal;

public record AmplitudePostingRequest(
        String paymentId,
        String debtorAccountReference,
        String treasuryAccountReference,
        BigDecimal amount,
        String currency,
        String financialInstitutionCode
) { }
