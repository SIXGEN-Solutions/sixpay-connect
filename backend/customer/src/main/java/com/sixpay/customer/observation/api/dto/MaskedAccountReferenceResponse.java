package com.sixpay.customer.observation.api.dto;

public record MaskedAccountReferenceResponse(
        String reference,
        String maskedValue
) {
}
