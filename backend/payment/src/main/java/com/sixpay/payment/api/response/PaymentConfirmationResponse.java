package com.sixpay.payment.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentConfirmationResponse(
        String paymentReference,
        String challengeStatus,
        String businessCode,
        String deliveryChannel,
        Instant sentAt,
        Instant expiresAt,
        Instant verifiedAt
) {
}
