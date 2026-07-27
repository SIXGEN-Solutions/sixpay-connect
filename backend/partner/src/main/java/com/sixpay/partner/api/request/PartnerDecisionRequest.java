package com.sixpay.partner.api.request;

import com.sixpay.partner.application.command.PartnerDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PartnerDecisionRequest(
        @NotNull PartnerDecision decision,
        @Size(max = 500) String reason
) {
}
