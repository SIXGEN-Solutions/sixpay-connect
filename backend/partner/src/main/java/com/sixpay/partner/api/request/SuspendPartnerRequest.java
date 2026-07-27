package com.sixpay.partner.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendPartnerRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
