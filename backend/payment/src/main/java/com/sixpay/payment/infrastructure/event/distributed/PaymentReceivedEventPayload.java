package com.sixpay.payment.infrastructure.event.distributed;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentReceivedEventPayload(
        String paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        BigDecimal amount,
        String currency,
        Instant receivedAt
) { }
