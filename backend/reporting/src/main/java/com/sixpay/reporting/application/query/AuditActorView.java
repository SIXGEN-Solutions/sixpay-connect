package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditActorType;

import java.util.List;
import java.util.Objects;

public record AuditActorView(
        AuditActorType actorType,
        String actorId,
        List<String> roles
) {
    public AuditActorView {
        actorType = Objects.requireNonNull(actorType, "actorType is required");
        actorId = Objects.requireNonNull(actorId, "actorId is required");
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
