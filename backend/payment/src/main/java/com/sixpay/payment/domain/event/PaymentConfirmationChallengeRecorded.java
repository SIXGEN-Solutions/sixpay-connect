package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.ConfirmationBusinessCode;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Records a change of the current bank-authoritative confirmation challenge.
 * Only the non-secret status and business code are emitted: no OTP, bank
 * account token, customer reference, or challenge payload leaves the aggregate.
 */
public record PaymentConfirmationChallengeRecorded(
        PaymentEventMetadata metadata,
        ConfirmationChallengeStatus challengeStatus,
        ConfirmationBusinessCode businessCode,
        Instant observedAt
) implements PaymentDomainEvent {
    public PaymentConfirmationChallengeRecorded {
        metadata = Objects.requireNonNull(metadata, "Event metadata");
        challengeStatus = Objects.requireNonNull(challengeStatus, "Challenge status");
        businessCode = Objects.requireNonNull(businessCode, "Business code");
        observedAt = Objects.requireNonNull(observedAt, "Observation instant");
    }
}
