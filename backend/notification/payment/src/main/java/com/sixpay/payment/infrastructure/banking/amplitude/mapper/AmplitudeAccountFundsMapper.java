package com.sixpay.payment.infrastructure.banking.amplitude.mapper;

import com.sixpay.payment.application.port.output.banking.FundsGateway;
import com.sixpay.payment.application.port.output.banking.VerificationGateway;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.infrastructure.banking.amplitude.dto.*;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class AmplitudeAccountFundsMapper {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AmplitudeAccountFundsMapper(
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.clock = Objects.requireNonNull(clock);
    }

    public AmplitudeAccountVerificationRequest toExternal(
            VerificationGateway.VerificationRequest request
    ) {
        return new AmplitudeAccountVerificationRequest(
                request.paymentId().toString(),
                request.customerIdentifier(),
                request.debtorAccountIdentifier(),
                request.context()
                        .financialInstitutionCode()
                        .toString()
        );
    }

    public AmplitudeFundsCheckRequest toExternal(
            FundsGateway.FundsCheckRequest request
    ) {
        return new AmplitudeFundsCheckRequest(
                request.paymentId().toString(),
                request.debtorAccountReference(),
                request.amount().amount(),
                request.amount().currency().getCurrencyCode(),
                request.context()
                        .financialInstitutionCode()
                        .toString()
        );
    }

    public BankingVerificationSnapshot toVerificationSnapshot(
            VerificationGateway.VerificationRequest request,
            AmplitudeAccountVerificationResponse response
    ) {
        requireVerificationResponse(response);

        Instant acceptedAt = clock.instant();
        Instant observedAt = notAfter(
                response.observedAt(),
                acceptedAt
        );

        List<BankingVerificationCheckEvidence> checks =
                response.checks().entrySet().stream()
                        .map(entry ->
                                new BankingVerificationCheckEvidence(
                                        BankingVerificationCheckType.valueOf(
                                                normalized(entry.getKey())
                                        ),
                                        evidenceResult(
                                                entry.getValue().result()
                                        ),
                                        reason(entry.getValue()),
                                        entry.getValue().checkedAt()
                                )
                        )
                        .toList();

        return new BankingVerificationSnapshot(
                new BankingVerificationId(
                        UUID.fromString(response.verificationId())
                ),
                BankingVerificationOutcome.valueOf(
                        normalized(response.outcome())
                ),
                response.accountBindingFingerprint(),
                checks,
                metadata(
                        request.context().correlationId(),
                        response,
                        observedAt,
                        acceptedAt
                )
        );
    }

    public FundsControlSnapshot toFundsSnapshot(
            FundsGateway.FundsCheckRequest request,
            AmplitudeFundsCheckResponse response
    ) {
        requireFundsResponse(response);

        Instant acceptedAt = clock.instant();
        Instant observedAt = notAfter(
                response.observedAt(),
                acceptedAt
        );

        List<FundsControlCheckEvidence> checks =
                response.checks().entrySet().stream()
                        .map(entry ->
                                new FundsControlCheckEvidence(
                                        FundsControlCheckType.valueOf(
                                                normalized(entry.getKey())
                                        ),
                                        evidenceResult(
                                                entry.getValue().result()
                                        ),
                                        reason(entry.getValue()),
                                        Objects.requireNonNullElse(
                                                entry.getValue().checkedAt(),
                                                observedAt
                                        )
                                )
                        )
                        .toList();

        return new FundsControlSnapshot(
                new FundsVerificationReference(
                        response.verificationReference()
                ),
                FundsControlOutcome.valueOf(
                        normalized(response.outcome())
                ),
                Money.of(
                        response.checkedAmount(),
                        response.currency()
                ),
                response.accountBindingFingerprint(),
                checks,
                Objects.requireNonNull(
                        response.validUntil(),
                        "Amplitude funds validUntil"
                ),
                metadata(
                        request.context().correlationId(),
                        response,
                        observedAt,
                        acceptedAt
                )
        );
    }

    private EvidenceMetadata metadata(
            com.sixpay.common.context.CorrelationId correlationId,
            Object response,
            Instant observedAt,
            Instant acceptedAt
    ) {
        return new EvidenceMetadata(
                ExternalSystem.AMPLITUDE,
                correlationId,
                EvidenceObservationChannel.DIRECT_RESPONSE,
                EvidenceFingerprint.of(
                        "v1:sha256:" + sha256(response)
                ),
                observedAt,
                acceptedAt
        );
    }

    private String sha256(Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(bytes);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Cannot fingerprint Amplitude evidence",
                    exception
            );
        }
    }

    private static FailureCode reason(
            AmplitudeCheckResult check
    ) {
        return check.reasonCode() == null
                || check.reasonCode().isBlank()
                ? null
                : FailureCode.of(
                        normalized(check.reasonCode())
                );
    }

    private static EvidenceCheckResult evidenceResult(
            String value
    ) {
        return EvidenceCheckResult.valueOf(
                normalized(value)
        );
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Amplitude value is required"
            );
        }
        return value.strip()
                .toUpperCase(Locale.ROOT);
    }

    private static Instant notAfter(
            Instant observedAt,
            Instant acceptedAt
    ) {
        Objects.requireNonNull(
                observedAt,
                "Amplitude observedAt"
        );
        return observedAt.isAfter(acceptedAt)
                ? acceptedAt
                : observedAt;
    }

    private static void requireVerificationResponse(
            AmplitudeAccountVerificationResponse response
    ) {
        Objects.requireNonNull(response, "Amplitude verification response");
        Objects.requireNonNull(response.checks(), "Amplitude verification checks");
        if (response.checks().isEmpty()) {
            throw new IllegalArgumentException(
                    "Amplitude verification checks are required"
            );
        }
    }

    private static void requireFundsResponse(
            AmplitudeFundsCheckResponse response
    ) {
        Objects.requireNonNull(response, "Amplitude funds response");
        Objects.requireNonNull(response.checks(), "Amplitude funds checks");
        if (response.checks().isEmpty()) {
            throw new IllegalArgumentException(
                    "Amplitude funds checks are required"
            );
        }
        Objects.requireNonNull(
                response.checkedAmount(),
                "Amplitude checked amount"
        );
    }
}
