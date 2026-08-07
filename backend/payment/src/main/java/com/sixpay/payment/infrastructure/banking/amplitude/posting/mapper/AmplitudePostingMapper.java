package com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper;

import com.sixpay.payment.application.port.output.banking.PostingGateway;
import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePostingRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePostingResponse;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class AmplitudePostingMapper {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AmplitudePostingMapper(
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public AmplitudePostingRequest toExternal(
            PostingGateway.PostingRequest request
    ) {
        return new AmplitudePostingRequest(
                request.paymentId().toString(),
                request.debtorAccountReference(),
                request.treasuryAccountReference().toString(),
                request.amount().amount(),
                request.amount().currency().getCurrencyCode(),
                request.context()
                        .financialInstitutionCode()
                        .toString()
        );
    }

    public PostingOutcomeSnapshot toSnapshot(
            PostingGateway.PostingRequest request,
            AmplitudePostingResponse response
    ) {
        Instant acceptedAt = clock.instant();
        Instant observedAt = response.observedAt() == null
                ? acceptedAt
                : response.observedAt().isAfter(acceptedAt)
                ? acceptedAt
                : response.observedAt();

        PostingOutcome outcome = PostingOutcome.valueOf(
                normalized(response.outcome())
        );

        BankPostingReference reference =
                response.principalPostingReference() == null
                        ? null
                        : new BankPostingReference(
                                response.principalPostingReference(),
                                response.debitLegReference(),
                                response.cutCreditLegReference()
                        );

        return new PostingOutcomeSnapshot(
                new PostingInstructionId(
                        UUID.fromString(
                                response.postingInstructionId()
                        )
                ),
                new PostingIdempotencyKey(
                        response.postingIdempotencyKey()
                ),
                outcome,
                reference,
                new PostingLegEvidence(
                        PostingLegStatus.valueOf(
                                normalized(
                                        response.debitLegStatus()
                                )
                        ),
                        response.debitLegReference(),
                        response.debitEffectiveAt(),
                        failureCode(
                                response.debitFailureCode()
                        )
                ),
                new PostingLegEvidence(
                        PostingLegStatus.valueOf(
                                normalized(
                                        response.cutCreditLegStatus()
                                )
                        ),
                        response.cutCreditLegReference(),
                        response.cutCreditEffectiveAt(),
                        failureCode(
                                response.cutCreditFailureCode()
                        )
                ),
                Money.of(
                        request.amount().amount(),
                        request.amount().currency()
                                .getCurrencyCode()
                ),
                response.businessDate(),
                failureCode(response.rejectionCode()),
                PostingNextAction.valueOf(
                        normalized(response.nextAction())
                ),
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        request.context().correlationId(),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
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
            byte[] bytes =
                    objectMapper.writeValueAsBytes(value);
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot fingerprint posting response",
                    exception
            );
        }
    }

    private static FailureCode failureCode(
            String value
    ) {
        return value == null || value.isBlank()
                ? null
                : FailureCode.of(normalized(value));
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Amplitude posting value is required"
            );
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
