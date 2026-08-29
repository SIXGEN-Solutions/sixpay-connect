package com.sixpay.payment.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One beneficiary allocation input an InitiateDebit request.
 */
public record InitiateDebitBeneficiaryRequest(
        @JsonProperty("rib")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9-]+$")
        String rib,

        @JsonProperty("montant")
        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 18, fraction = 2)
        BigDecimal amount
) {
}
