package com.sixpay.payment.infrastructure.banking.amplitude.status.mapper;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.EvidenceFingerprint;
import com.sixpay.payment.domain.model.evidence.EvidenceMetadata;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import com.sixpay.payment.domain.model.evidence.PostingIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;
import com.sixpay.payment.domain.model.evidence.PostingLegEvidence;
import com.sixpay.payment.domain.model.evidence.PostingLegStatus;
import com.sixpay.payment.domain.model.evidence.PostingNextAction;
import com.sixpay.payment.domain.model.evidence.PostingOutcome;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.infrastructure.banking.amplitude.status.dto.AmplitudePostingStatusResponse;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class AmplitudePostingStatusMapper {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AmplitudePostingStatusMapper(
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper
        );
        this.clock = Objects.requireNonNull(clock);
    }

    public PostingOutcomeSnapshot toSnapshot(
            AmplitudePostingStatusResponse response,
            CorrelationId correlationId,
            EvidenceObservationChannel channel
    ) {
        Objects.requireNonNull(
                response,
                "Amplitude posting status response"
        );
        Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );

        if (channel
                != EvidenceObservationChannel.IDEMPOTENCY_LOOKUP
                && channel
                != EvidenceObservationChannel.BANK_REFERENCE_LOOKUP) {
            throw new IllegalArgumentException(
                    "Posting status channel must be a lookup channel"
            );
        }

        Instant acceptedAt = clock.instant();
        Instant observedAt =
                response.observedAt() == null
                        ? acceptedAt
                        : response.observedAt()
                                .isAfter(acceptedAt)
                        ? acceptedAt
                        : response.observedAt();

        BankPostingReference bankReference =
                response.principalPostingReference() == null
                        || response
                        .principalPostingReference()
                        .isBlank()
                        ? null
                        : new BankPostingReference(
                                response
                                        .principalPostingReference(),
                                nullableText(
                                        response
                                                .debitLegReference()
                                ),
                                nullableText(
                                        response
                                                .cutCreditLegReference()
                                )
                        );

        return new PostingOutcomeSnapshot(
                new PostingInstructionId(
                        UUID.fromString(
                                required(
                                        response
                                                .postingInstructionId(),
                                        "postingInstructionId"
                                )
                        )
                ),
                new PostingIdempotencyKey(
                        required(
                                response
                                        .postingIdempotencyKey(),
                                "postingIdempotencyKey"
                        )
                ),
                PostingOutcome.valueOf(
                        normalized(
                                response.outcome()
                        )
                ),
                bankReference,
                new PostingLegEvidence(
                        PostingLegStatus.valueOf(
                                normalized(
                                        response
                                                .debitLegStatus()
                                )
                        ),
                        nullableText(
                                response
                                        .debitLegReference()
                        ),
                        response.debitEffectiveAt(),
                        failureCode(
                                response
                                        .debitFailureCode()
                        )
                ),
                new PostingLegEvidence(
                        PostingLegStatus.valueOf(
                                normalized(
                                        response
                                                .cutCreditLegStatus()
                                )
                        ),
                        nullableText(
                                response
                                        .cutCreditLegReference()
                        ),
                        response.cutCreditEffectiveAt(),
                        failureCode(
                                response
                                        .cutCreditFailureCode()
                        )
                ),
                Money.of(
                        Objects.requireNonNull(
                                response.amount(),
                                "Posting amount"
                        ),
                        required(
                                response.currency(),
                                "currency"
                        )
                ),
                response.businessDate(),
                failureCode(
                        response.rejectionCode()
                ),
                PostingNextAction.valueOf(
                        normalized(
                                response.nextAction()
                        )
                ),
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        correlationId,
                        channel,
                        EvidenceFingerprint.of(
                                "v1:sha256:"
                                        + fingerprint(response)
                        ),
                        observedAt,
                        acceptedAt
                )
        );
    }

    private String fingerprint(Object value) {
        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            objectMapper
                                    .writeValueAsBytes(value)
                    );

            return HexFormat.of()
                    .formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot fingerprint posting-status response",
                    exception
            );
        }
    }

    private static FailureCode failureCode(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : FailureCode.of(
                        normalized(value)
                );
    }

    private static String nullableText(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }

    private static String normalized(
            String value
    ) {
        return required(value, "value")
                .toUpperCase(Locale.ROOT);
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Amplitude " + name + " is required"
            );
        }

        return value.strip();
    }
}
