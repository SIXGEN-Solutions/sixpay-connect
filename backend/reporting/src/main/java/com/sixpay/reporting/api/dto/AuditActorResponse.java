package com.sixpay.reporting.api.dto;

import java.util.List;

public record AuditActorResponse(
        String actorType,
        String actorId,
        List<String> roles
) {
}
