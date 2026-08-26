package com.sixpay.accounting.application.port.output;

import com.sixpay.common.context.CorrelationId;

import java.util.Objects;
import java.util.UUID;

public record AccountingIntegrationContext(
        CorrelationId correlationId,
        UUID requestId
) {
    public AccountingIntegrationContext {
        correlationId = Objects.requireNonNull(
                correlationId,
                "correlationId"
        );
        requestId = Objects.requireNonNull(
                requestId,
                "requestId"
        );
    }

    public static AccountingIntegrationContext create(
            CorrelationId correlationId
    ) {
        return new AccountingIntegrationContext(
                correlationId,
                UUID.randomUUID()
        );
    }
}
