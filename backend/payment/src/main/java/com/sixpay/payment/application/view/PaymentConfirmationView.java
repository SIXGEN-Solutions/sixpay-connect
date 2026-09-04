package com.sixpay.payment.application.view;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Application projection intended for the public Payment-confirmation API.
 *
 * <p>The internal challengeReference is deliberately absent.</p>
 */
public record PaymentConfirmationView(
        PublicPaymentReference paymentReference,
        ConfirmationChallengeStatus status,
        ConfirmationBusinessCode businessCode,
        ConfirmationDeliveryChannel deliveryChannel,
        Instant sentAt,
        Instant expiresAt,
        Instant verifiedAt,
        boolean replayed
) {
    public PaymentConfirmationView {
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        status = Objects.requireNonNull(
                status,
                "Confirmation challenge status"
        );
        businessCode = Objects.requireNonNull(
                businessCode,
                "Confirmation business code"
        );
    }

    public PaymentConfirmationView(
            PublicPaymentReference paymentReference,
            ConfirmationChallengeStatus status,
            ConfirmationBusinessCode businessCode,
            ConfirmationDeliveryChannel deliveryChannel,
            Instant sentAt,
            Instant expiresAt,
            Instant verifiedAt
    ) {
        this(
                paymentReference,
                status,
                businessCode,
                deliveryChannel,
                sentAt,
                expiresAt,
                verifiedAt,
                false
        );
    }

    public static PaymentConfirmationView from(
            PublicPaymentReference paymentReference,
            PaymentConfirmationBankResult result
    ) {
        return from(paymentReference, result, false);
    }

    public static PaymentConfirmationView from(
            PublicPaymentReference paymentReference,
            PaymentConfirmationBankResult result,
            boolean replayed
    ) {
        Objects.requireNonNull(result, "Bank confirmation result");

        return new PaymentConfirmationView(
                paymentReference,
                result.status(),
                result.businessCode(),
                result.deliveryChannel(),
                result.sentAt(),
                result.expiresAt(),
                result.verifiedAt(),
                replayed
        );
    }

    public Optional<ConfirmationDeliveryChannel> optionalDeliveryChannel() {
        return Optional.ofNullable(deliveryChannel);
    }

    public Optional<Instant> optionalSentAt() {
        return Optional.ofNullable(sentAt);
    }

    public Optional<Instant> optionalExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public Optional<Instant> optionalVerifiedAt() {
        return Optional.ofNullable(verifiedAt);
    }
}
