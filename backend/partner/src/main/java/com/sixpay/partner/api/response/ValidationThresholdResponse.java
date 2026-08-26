package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.ValidationThresholdView;

import java.math.BigDecimal;

/**
 * Public API representation of a partner validation threshold.
 *
 * <p>This DTO prevents the HTTP contract from exposing the application-layer
 * projection directly.</p>
 */
public record ValidationThresholdResponse(
        String transactionType,
        String currency,
        BigDecimal amount,
        int validationLevels
) {

    public static ValidationThresholdResponse from(ValidationThresholdView view) {
        return new ValidationThresholdResponse(
                view.transactionType(),
                view.currency(),
                view.amount(),
                view.validationLevels()
        );
    }
}