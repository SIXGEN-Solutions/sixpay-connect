package com.sixpay.payment.api.response;

import java.math.BigDecimal;

/**
 * Contract-safe monetary representation.
 */
public record PaymentMoneyResponse(
        BigDecimal amount,
        String currency
) {
}
