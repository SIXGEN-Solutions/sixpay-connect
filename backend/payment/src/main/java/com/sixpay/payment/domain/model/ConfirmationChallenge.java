package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Current authoritative Payment-confirmation challenge known by SIXPAY.
 *
 * <p>This is subordinate state of {@link Payment}; it is not an Aggregate
 * Root and does not own OTP generation, storage or verification. Challenge
 * lifecycle decisions remain authoritative in Amplitude / La Regionale.</p>
 *
 * <p>No OTP value, raw OTP hash or reusable authentication secret may be
 * stored in this object.</p>
 */
public record ConfirmationChallenge(
        ConfirmationChallengeReference challengeReference,
        ConfirmationChallengeBinding binding,
        ConfirmationChallengeStatus status,
        ConfirmationBusinessCode businessCode,
        ConfirmationDeliveryChannel deliveryChannel,
        Instant sentAt,
        Instant expiresAt,
        Instant verifiedAt
) implements ValueObject {

    public ConfirmationChallenge {
        challengeReference = Objects.requireNonNull(
                challengeReference,
                "Confirmation challenge reference"
        );
        binding = Objects.requireNonNull(
                binding,
                "Confirmation challenge binding"
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

    public boolean active() {
        return status == ConfirmationChallengeStatus.ACTIVE;
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

    @Override
    public String toString() {
        return "ConfirmationChallenge[challengeReference="
                + challengeReference
                + ", paymentReference="
                + binding.paymentReference()
                + ", status="
                + status
                + ", businessCode="
                + businessCode
                + "]";
    }
}
