package com.sixpay.partner.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreatePartnerRequest(
        @NotBlank @Size(max = 200) String legalName,
        @NotBlank @Size(max = 150) String technicalContactName,
        @NotBlank @Email @Size(max = 254) String technicalContactEmail,
        @NotEmpty Set<@NotBlank @Size(max = 64) String> authorizedTransactionTypes
) {
}
