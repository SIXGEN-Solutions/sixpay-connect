package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.math.BigDecimal;

public record AmplitudePaymentProviderEntry(
        int sequence,
        String direction,
        String accountReference,
        BigDecimal amount,
        String currency,
        String paymentReference
) { }
