package com.sixpay.payment.infrastructure.banking.amplitude.reservation.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AmplitudeFundsReservationResponse(
        String code,
        String outcome,
        String reservationReference,
        BigDecimal reservedAmount,
        String currency,
        String accountBindingFingerprint,
        Instant observedAt,
        Instant expiresAt,
        String reasonCode
) { }
