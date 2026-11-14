package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.command.InitiateDebitCommand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Produces the deterministic canonical representation hashed for InitiateDebit.
 */
@Component
public final class PaymentInitiationCanonicalizer {

    /**
     * Builds an unambiguous, order-independent representation of the business
     * request.
     *
     * <p>Beneficiaries are never provided by Payment initiation: any later
     * Treasury configuration is resolved independently of this fingerprint.
     * Length prefixes prevent delimiter ambiguity. Correlation and idempotency
     * keys are excluded as execution metadata.</p>
     */
    public String canonicalize(InitiateDebitCommand command) {
        Objects.requireNonNull(command, "InitiateDebit command");

        StringBuilder value = new StringBuilder(1024);

        // AppID is the canonical Partner business identifier.
        // LoginName is compatibility metadata and the authenticated subject
        // is transport/security identity; neither changes the business request.
        append(value, command.resolvedPartnerIdentifier());
        append(value, command.clientReference());
        append(value, command.applicationId());
        append(value, command.endToEndId());
        append(value, decimal(command.totalAmount()));
        append(value, command.currency());
        append(value, command.debtorRib());
        append(value, command.debtorName());
        append(value, command.claimType().name());
        append(value, command.taxpayerIdentifier());
        append(value, command.requestedExecutionAt() == null
                ? null : command.requestedExecutionAt().toString());
        append(value, command.callbackUrl());

        return value.toString();
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static void append(
            StringBuilder target,
            String value
    ) {
        if (value == null) {
            target.append("N|");
            return;
        }
        target.append('V')
                .append(value.length())
                .append(':')
                .append(value)
                .append('|');
    }
}
