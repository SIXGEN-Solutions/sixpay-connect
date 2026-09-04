package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto;

import java.time.Instant;

public record AmplitudeConfirmationResponse(
        String paymentReference,
        String challengeReference,
        String challengeStatus,
        String businessCode,
        String deliveryChannel,
        Instant sentAt,
        Instant expiresAt,
        Instant verifiedAt
) { }
