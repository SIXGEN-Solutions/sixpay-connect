package com.sixpay.payment.infrastructure.event.distributed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentPostedEventPayload(
        String paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        BigDecimal amount,
        String currency,
        String postingOutcome,
        String bankPostingReference,
        LocalDate businessDate,
        Instant postedAt
) { }
