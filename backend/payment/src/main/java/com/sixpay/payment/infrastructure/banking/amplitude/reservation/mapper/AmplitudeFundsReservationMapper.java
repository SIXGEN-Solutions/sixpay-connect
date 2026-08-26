package com.sixpay.payment.infrastructure.banking.amplitude.reservation.mapper;

import com.sixpay.payment.application.port.output.banking.FundsReservationGateway;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.dto.AmplitudeFundsReservationRequest;
import com.sixpay.payment.infrastructure.banking.amplitude.reservation.dto.AmplitudeFundsReservationResponse;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public final class AmplitudeFundsReservationMapper {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AmplitudeFundsReservationMapper(
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public AmplitudeFundsReservationRequest toExternal(
            FundsReservationGateway.FundsReservationRequest request
    ) {
        return new AmplitudeFundsReservationRequest(
                request.paymentId().toString(),
                request.debtorAccountReference(),
                request.amount().amount(),
                request.amount().currency().getCurrencyCode(),
                request.context()
                        .financialInstitutionCode()
                        .toString()
        );
    }

    public FundsReservationSnapshot toSnapshot(
            FundsReservationGateway.FundsReservationRequest request,
            AmplitudeFundsReservationResponse response
    ) {
        FundsReservationOutcome outcome =
                FundsReservationOutcome.valueOf(
                        response.outcome()
                                .strip()
                                .toUpperCase(Locale.ROOT)
                );

        Instant acceptedAt = clock.instant();
        Instant observedAt = response.observedAt()
                .isAfter(acceptedAt)
                ? acceptedAt
                : response.observedAt();

        return new FundsReservationSnapshot(
                outcome,
                outcome == FundsReservationOutcome.RESERVED
                        ? new FundsReservationReference(
                                response.reservationReference()
                        )
                        : null,
                Money.of(
                        response.reservedAmount(),
                        response.currency()
                ),
                response.accountBindingFingerprint(),
                outcome == FundsReservationOutcome.RESERVED
                        ? response.expiresAt()
                        : null,
                outcome == FundsReservationOutcome.REJECTED
                        ? FailureCode.of(
                                response.reasonCode()
                                        .strip()
                                        .toUpperCase(Locale.ROOT)
                        )
                        : null,
                new EvidenceMetadata(
                        ExternalSystem.AMPLITUDE,
                        request.context().correlationId(),
                        EvidenceObservationChannel.DIRECT_RESPONSE,
                        EvidenceFingerprint.of(
                                "v1:sha256:" + fingerprint(response)
                        ),
                        observedAt,
                        acceptedAt
                )
        );
    }

    private String fingerprint(Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot fingerprint reservation response",
                    exception
            );
        }
    }
}
