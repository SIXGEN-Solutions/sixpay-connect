package com.sixpay.partner.application.view;

import com.sixpay.partner.domain.model.ValidationThreshold;

import java.math.BigDecimal;

public record ValidationThresholdView(
        String transactionType,
        String currency,
        BigDecimal amount,
        int validationLevels
) {

    public static ValidationThresholdView from(ValidationThreshold threshold) {
        return new ValidationThresholdView(
                threshold.transactionType(),
                threshold.currency(),
                threshold.amount(),
                threshold.validationLevels()
        );
    }
}
