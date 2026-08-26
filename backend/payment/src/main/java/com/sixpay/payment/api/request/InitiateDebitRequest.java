package com.sixpay.payment.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixpay.payment.domain.model.ClaimType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * TresorPay request for the contracted InitiateDebit operation.
 */
public record InitiateDebitRequest(
        @JsonProperty("LoginName")
        @NotBlank
        @Size(max = 64)
        String loginName,

        @JsonProperty("AppID")
        @Size(max = 64)
        String applicationId,

        @JsonProperty("endToEndId")
        @NotBlank
        @Size(max = 128)
        String endToEndId,

        @JsonProperty("montantTotal")
        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 18, fraction = 2)
        BigDecimal totalAmount,

        @JsonProperty("devise")
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        @JsonProperty("ribDebiteur")
        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9-]+$")
        String debtorRib,

        @JsonProperty("nomDebiteur")
        @NotBlank
        @Size(max = 200)
        String debtorName,

        @JsonProperty("typeCreance")
        @NotNull
        ClaimType claimType,

        @JsonProperty("NUI")
        @NotBlank
        @Size(max = 64)
        String taxpayerIdentifier,

        @JsonProperty("dateExecution")
        @NotNull
        Instant requestedExecutionAt,

        @JsonProperty("beneficiaires")
        @NotEmpty
        @Size(max = 20)
        List<@Valid InitiateDebitBeneficiaryRequest> beneficiaries,

        @JsonProperty("callbackURL")
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https://.+$")
        String callbackUrl
) {

    public InitiateDebitRequest {
        beneficiaries = beneficiaries == null
                ? null
                : List.copyOf(beneficiaries);
    }
}
