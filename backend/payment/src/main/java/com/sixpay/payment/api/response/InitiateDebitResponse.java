package com.sixpay.payment.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixpay.payment.domain.model.PaymentStatus;

import java.time.Instant;

/**
 * TresorPay response for InitiateDebit.
 *
 * <p>Bank-issued challenge fields remain absent until an approved core-banking
 * contract provides them. SIXPAY does not fabricate these values.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InitiateDebitResponse(
        @JsonProperty("OK")
        String ok,

        @JsonProperty("Description")
        String description,

        @JsonProperty("Result")
        String result,

        String paymentReference,

        String endToEndId,

        String bankOperationId,

        @JsonProperty("montantTotal")
        PaymentMoneyResponse totalAmount,

        @JsonProperty("frais")
        PaymentMoneyResponse fees,

        @JsonProperty("montantNet")
        PaymentMoneyResponse netAmount,

        @JsonProperty("Date")
        Instant initiatedAt,

        @JsonProperty("ValidityInMinutes")
        Integer validityInMinutes,

        @JsonProperty("TransactionNumber")
        String transactionNumber,

        @JsonProperty("TransactionQRCode")
        String transactionQrCode,

        @JsonProperty("Status")
        PaymentStatus status,

        @JsonProperty("NextStep")
        String nextStep
) {
}
