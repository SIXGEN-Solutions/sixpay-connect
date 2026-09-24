package com.sixpay.accounting.infrastructure.partnerstatus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record PartnerPaymentStatusResponseDto(
        String reference,
        @JsonProperty("transaction_id")
        String transactionId,
        String status,
        @JsonProperty("payment_method")
        String paymentMethod,
        @JsonProperty("operator_reference")
        String operatorReference,
        @JsonProperty("debit_effectue")
        boolean debitEffectue,
        @JsonProperty("quittance_disponible")
        boolean quittanceDisponible,
        @JsonProperty("updated_at")
        Instant updatedAt,
        @JsonProperty("failure_reason")
        String failureReason
) {
}
