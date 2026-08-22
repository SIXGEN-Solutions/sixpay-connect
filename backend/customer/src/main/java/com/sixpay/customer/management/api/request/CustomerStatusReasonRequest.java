package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerStatusReasonRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
