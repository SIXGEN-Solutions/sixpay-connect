package com.sixpay.customer.management.application.port.input;

public record AddBankAccountCommand(
        String accountReference,
        String correlationId
) {
    public AddBankAccountCommand {
        if (accountReference == null || accountReference.isBlank()) {
            throw new IllegalArgumentException(
                    "accountReference is required"
            );
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException(
                    "correlationId is required"
            );
        }

        accountReference = accountReference.strip();
        correlationId = correlationId.strip();
    }
}
