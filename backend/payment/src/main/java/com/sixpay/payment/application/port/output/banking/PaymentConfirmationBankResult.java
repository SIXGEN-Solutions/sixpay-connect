package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeReference;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.ConfirmationDeliveryChannel;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider-neutral authoritative result of a banking confirmation operation.
 *
 * <p>The challenge reference remains internal to the Payment application and
 * is not part of the public TRESOR PAY view.</p>
 */
public record PaymentConfirmationBankResult(
        ConfirmationChallengeReference challengeReference,
        ConfirmationChallengeStatus status,
        ConfirmationBusinessCode businessCode,
        ConfirmationDeliveryChannel deliveryChannel,
        Instant sentAt,
        Instant expiresAt,
        Instant verifiedAt
) {
    public PaymentConfirmationBankResult {
        challengeReference = Objects.requireNonNull(
                challengeReference,
                "Confirmation challenge reference"
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
