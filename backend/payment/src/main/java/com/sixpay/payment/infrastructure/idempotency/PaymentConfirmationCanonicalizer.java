package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class PaymentConfirmationCanonicalizer {

    private static final String VERSION = "v1";

    public String create(
            PaymentId paymentId,
            PublicPaymentReference paymentReference
    ) {
        return String.join(
                "|",
                VERSION,
                "CREATE",
                Objects.requireNonNull(paymentId, "Payment ID").toString(),
                Objects.requireNonNull(
                        paymentReference,
                        "Payment reference"
                ).value()
        );
    }

    public String replace(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference
    ) {
        return String.join(
                "|",
                VERSION,
                "REPLACE",
                Objects.requireNonNull(paymentId, "Payment ID").toString(),
                Objects.requireNonNull(
                        paymentReference,
                        "Payment reference"
                ).value(),
                Objects.requireNonNull(
                        challengeReference,
                        "Challenge reference"
                ).value()
        );
    }

    public String revoke(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            ConfirmationChallengeReference challengeReference,
            String reasonCode
    ) {
        String canonicalReason = Objects.requireNonNull(
                reasonCode,
                "Revocation reason code"
        );
        if (canonicalReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Revocation reason code must not be blank"
            );
        }

        return String.join(
                "|",
                VERSION,
                "REVOKE",
                Objects.requireNonNull(paymentId, "Payment ID").toString(),
                Objects.requireNonNull(
                        paymentReference,
                        "Payment reference"
                ).value(),
                Objects.requireNonNull(
                        challengeReference,
                        "Challenge reference"
                ).value(),
                canonicalReason
        );
    }
}
