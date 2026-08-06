package com.sixpay.payment.infrastructure.banking.amplitude.dto;

import java.math.BigDecimal;

public record AmplitudeFundsCheckRequest(
        String paymentId,
        String debtorAccountReference,
        BigDecimal amount,
        String currency,
        String financialInstitutionCode
) { }
