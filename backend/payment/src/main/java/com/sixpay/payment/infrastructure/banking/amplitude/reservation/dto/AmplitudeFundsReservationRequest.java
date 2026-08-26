package com.sixpay.payment.infrastructure.banking.amplitude.reservation.dto;

import java.math.BigDecimal;

public record AmplitudeFundsReservationRequest(
        String paymentId,
        String debtorAccountReference,
        BigDecimal amount,
        String currency,
        String financialInstitutionCode
) { }
