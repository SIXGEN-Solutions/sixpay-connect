package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.application.command.InitiateDebitBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
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
     * <p>Beneficiaries are sorted so equivalent allocations produce the same
     * fingerprint. Length prefixes prevent delimiter ambiguity. Correlation
     * and idempotency keys are intentionally excluded because they identify
     * transport/execution context rather than the requested debit itself.</p>
     */
    public String canonicalize(InitiateDebitCommand command) {
        Objects.requireNonNull(command, "InitiateDebit command");

        StringBuilder value = new StringBuilder(1024);

        append(value, command.partnerLoginName());
        append(value, command.authenticatedPartnerLoginName());
        append(value, command.applicationId());
        append(value, command.endToEndId());
        append(value, decimal(command.totalAmount()));
        append(value, command.currency());
        append(value, command.debtorRib());
        append(value, command.debtorName());
        append(value, command.claimType().name());
        append(value, command.taxpayerIdentifier());
        append(value, command.requestedExecutionAt().toString());
        append(value, command.callbackUrl());

        command.beneficiaries().stream()
                .sorted(
                        Comparator.comparing(
                                InitiateDebitBeneficiaryCommand::rib
                        ).thenComparing(
                                beneficiary ->
                                        decimal(
                                                beneficiary.amount()
                                        )
                        )
                )
                .forEach(beneficiary -> {
                    append(value, beneficiary.rib());
                    append(
                            value,
                            decimal(beneficiary.amount())
                    );
                });

        return value.toString();
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static void append(
            StringBuilder target,
            String value
    ) {
        String normalized = value == null ? "" : value;
        target.append(normalized.length())
                .append(':')
                .append(normalized)
                .append('|');
    }
}
