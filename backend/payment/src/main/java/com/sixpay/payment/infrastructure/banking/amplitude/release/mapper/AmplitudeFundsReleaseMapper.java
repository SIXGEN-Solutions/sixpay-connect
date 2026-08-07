package com.sixpay.payment.infrastructure.banking.amplitude.release.mapper;

import com.sixpay.payment.application.port.output.banking.FundsReleaseGateway;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.infrastructure.banking.amplitude.release.dto.*;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public final class AmplitudeFundsReleaseMapper {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AmplitudeFundsReleaseMapper(
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public AmplitudeFundsReleaseRequest toExternal(
            FundsReleaseGateway.FundsReleaseRequest request
    ) {
        return new AmplitudeFundsReleaseRequest(
                request.paymentId().toString(),
                request.reservationReference().toString(),
                request.reasonCode(),
                request.context()
                        .financialInstitutionCode()
                        .toString()
        );
    }

    public FundsReleaseSnapshot toSnapshot(
            AmplitudeFundsReleaseResponse response,
            com.sixpay.common.context.CorrelationId correlationId
    ) {
        Instant acceptedAt = clock.instant();
        Instant observedAt = response.observedAt() == null
                ? acceptedAt
                : response.observedAt();

        FundsReleaseOutcome outcome =
                FundsReleaseOutcome.valueOf(
                        response.outcome()
                                .strip()
                                .toUpperCase(Locale.ROOT)
                );

        return new FundsReleaseSnapshot(
                new FundsReservationReference(
                        response.reservationReference()
                ),
                outcome,
                response.releaseReference(),
                response.failureCode() == null
                        ? null
                        : FailureCode.of(
                                response.failureCode()
                                        .strip()
                                        .toUpperCase(Locale.ROOT)
                        ),
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        correlationId,
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
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    objectMapper
                                            .writeValueAsBytes(value)
                            )
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot fingerprint release response",
                    exception
            );
        }
    }
}
