package com.sixpay.payment.infrastructure.banking.amplitude.reversal.mapper;

import com.sixpay.payment.application.port.output.banking.ReversalGateway;
import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.dto.*;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class AmplitudeReversalMapper {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AmplitudeReversalMapper(
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public AmplitudeReversalRequest toExternal(
            ReversalGateway.ReversalRequest request
    ) {
        var authorization = request.authorization();

        return new AmplitudeReversalRequest(
                request.paymentId().toString(),
                request.bankPostingReference(),
                authorization.authorizationType().name(),
                authorization.authorizationReference().toString(),
                authorization.requestedBySubject(),
                authorization.reasonCode().toString(),
                authorization.authorizedAt(),
                authorization.requestedAt(),
                request.context()
                        .financialInstitutionCode()
                        .toString()
        );
    }

    public ReversalSnapshot toSnapshot(
            ReversalGateway.ReversalRequest request,
            AmplitudeReversalResponse response
    ) {
        Instant acceptedAt = clock.instant();
        Instant observedAt = response.observedAt() == null
                ? acceptedAt
                : response.observedAt();

        ReversalOutcome outcome = ReversalOutcome.valueOf(
                response.outcome()
                        .strip()
                        .toUpperCase(Locale.ROOT)
        );

        ReversalOutcomeEvidence evidence =
                new ReversalOutcomeEvidence(
                        response.reversalReference() == null
                                ? null
                                : new ReversalReference(
                                        response.reversalReference()
                                ),
                        outcome,
                        response.reversalEntryReference(),
                        response.failureCode() == null
                                ? null
                                : FailureCode.of(
                                        response.failureCode()
                                                .strip()
                                                .toUpperCase(Locale.ROOT)
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

        return new ReversalSnapshot(
                new BankPostingReference(
                        response.originalBankPostingReference(),
                        null,
                        null
                ),
                new ReversalInstructionId(
                        UUID.fromString(
                                response.reversalInstructionId()
                        )
                ),
                new ReversalIdempotencyKey(
                        response.reversalIdempotencyKey()
                ),
                request.authorization(),
                evidence
        );
    }

    private String fingerprint(Object value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    objectMapper
                                            .writeValueAsBytes(value)
                            )
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot fingerprint reversal response",
                    exception
            );
        }
    }
}
