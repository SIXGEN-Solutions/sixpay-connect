package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;

import java.util.Objects;

public interface VerificationGateway {

    BankingVerificationSnapshot verify(VerificationRequest request);

    record VerificationRequest(
            PaymentId paymentId,
            BankingRequestContext context,
            String customerIdentifier,
            String debtorAccountIdentifier
    ) {
        public VerificationRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            context = Objects.requireNonNull(context, "Banking request context");
            customerIdentifier = requireText(
                    customerIdentifier,
                    "Customer identifier"
            );
            debtorAccountIdentifier = requireText(
                    debtorAccountIdentifier,
                    "Debtor account identifier"
            );
        }

        private static String requireText(String value, String label) {
            if (value == null || value.isBlank() || value.length() > 100) {
                throw new IllegalArgumentException(
                        label + " must be non-blank and at most 100 characters"
                );
            }
            return value;
        }
    }
}
