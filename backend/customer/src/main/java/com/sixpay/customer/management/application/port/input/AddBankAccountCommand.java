package com.sixpay.customer.management.application.port.input;

import java.time.Instant;

public record AddBankAccountCommand(
        String bankingAccountReference,
        String accountBindingFingerprint,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        Instant verifiedAt
) {
}
