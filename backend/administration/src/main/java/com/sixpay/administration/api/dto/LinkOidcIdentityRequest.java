package com.sixpay.administration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkOidcIdentityRequest(
        @NotBlank @Size(max = 500) String provider,
        @NotBlank @Size(max = 255) String providerSubject
) {
}
