package com.sixpay.payment.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixpay.payment.domain.model.ClaimType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TresorPay request for the contracted InitiateDebit operation.
 */
public record InitiateDebitRequest(
        @JsonProperty("LoginName")
        @Size(max = 64)
        String loginName,

        @JsonProperty("AppID")
        @NotBlank
        @Size(max = 64)
        String applicationId,

        @JsonProperty("referenceClient")
        @NotBlank
        @Size(max = 128)
        String clientReference,

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
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,

        @JsonProperty("ribDebiteur")
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^[A-Za-z0-9-]+$")
        String debtorRib,

        @JsonProperty("nomDebiteur")
        @Size(max = 200)
        String debtorName,

        @JsonProperty("typeCreance")
        @NotNull
        ClaimType claimType,

        @JsonProperty("NUI")
        @Size(max = 64)
        String taxpayerIdentifier,

        @JsonProperty("dateExecution")
        Instant requestedExecutionAt,

        @JsonProperty("callbackURL")
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https://.+$")
        String callbackUrl
) {

    /**
     * Legacy Java constructor retained only for source compatibility.
     * Beneficiaries are deliberately not part of the public request schema.
     */
    @Deprecated
    public InitiateDebitRequest(
            String loginName, String applicationId, String clientReference,
            String endToEndId, BigDecimal totalAmount, String currency,
            String debtorRib, String debtorName, ClaimType claimType,
            String taxpayerIdentifier, Instant requestedExecutionAt,
            java.util.List<InitiateDebitBeneficiaryRequest> ignoredBeneficiaries,
            String callbackUrl
    ) {
        this(loginName, applicationId, clientReference, endToEndId,
                totalAmount, currency, debtorRib, debtorName, claimType,
                taxpayerIdentifier, requestedExecutionAt, callbackUrl);
    }

    @Deprecated
    public InitiateDebitRequest(
            String loginName, String applicationId, String endToEndId,
            BigDecimal totalAmount, String currency, String debtorRib,
            String debtorName, ClaimType claimType, String taxpayerIdentifier,
            Instant requestedExecutionAt,
            java.util.List<InitiateDebitBeneficiaryRequest> ignoredBeneficiaries,
            String callbackUrl
    ) {
        this(loginName, applicationId, endToEndId, endToEndId,
                totalAmount, currency, debtorRib, debtorName, claimType,
                taxpayerIdentifier, requestedExecutionAt, callbackUrl);
    }
}
