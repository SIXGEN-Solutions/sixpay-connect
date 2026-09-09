package com.sixpay.accounting.infrastructure.tfj.mapper;

import com.sixpay.accounting.domain.model.TfjConfirmation;
import com.sixpay.accounting.domain.model.TfjMatchStatus;
import com.sixpay.accounting.domain.model.TfjObservationChannel;
import com.sixpay.accounting.domain.model.TfjRecoveryAction;
import com.sixpay.accounting.domain.model.TfjStatus;
import com.sixpay.accounting.infrastructure.tfj.dto.EndOfDayConfirmationDto;

import java.time.Clock;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AmplitudeTfjMapper {

    private static final Pattern FI_CODE =
            Pattern.compile("^[A-Z0-9._-]{2,35}$");
    private static final Pattern PROVIDER_REFERENCE =
            Pattern.compile("^[A-Za-z0-9._:/-]{1,100}$");

    private final Clock clock;

    public AmplitudeTfjMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public TfjConfirmation toDomain(
            EndOfDayConfirmationDto dto,
            String idempotencyKey,
            String correlationId,
            TfjObservationChannel channel
    ) {
        Objects.requireNonNull(dto, "dto");
        if (!"1.0".equals(dto.schemaVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported TFJ schemaVersion"
            );
        }

        String institutionCode =
                match(
                        dto.financialInstitutionCode(),
                        FI_CODE,
                        "financialInstitutionCode"
                );
        String paymentReference =
                match(
                        dto.paymentReference(),
                        PROVIDER_REFERENCE,
                        "paymentReference"
                );
        String bankPostingReference =
                match(
                        dto.bankPostingReference(),
                        PROVIDER_REFERENCE,
                        "bankPostingReference"
                );

        TfjStatus status = TfjStatus.valueOf(
                required(dto.tfjStatus(), "tfjStatus")
        );

        String failureCode = null;
        String failureDescription = null;
        TfjRecoveryAction recoveryAction = null;

        if (dto.failure() != null) {
            failureCode = required(
                    dto.failure().code(),
                    "failure.code"
            );
            failureDescription = required(
                    dto.failure().description(),
                    "failure.description"
            );
            if (failureDescription.length() > 500) {
                throw new IllegalArgumentException(
                        "failure.description exceeds 500 characters"
                );
            }
            recoveryAction = TfjRecoveryAction.valueOf(
                    required(
                            dto.failure().recoveryAction(),
                            "failure.recoveryAction"
                    )
            );
        }

        return new TfjConfirmation(
                Objects.requireNonNull(
                        dto.confirmationId(),
                        "confirmationId"
                ),
                required(idempotencyKey, "idempotencyKey"),
                "pending",
                institutionCode,
                Objects.requireNonNull(
                        dto.businessDate(),
                        "businessDate"
                ),
                paymentReference,
                bankPostingReference,
                dto.tfjBatchReference(),
                status,
                Objects.requireNonNull(
                        dto.confirmedAt(),
                        "confirmedAt"
                ),
                failureCode,
                failureDescription,
                recoveryAction,
                clock.instant(),
                Objects.requireNonNull(channel, "channel"),
                required(correlationId, "correlationId"),
                TfjMatchStatus.UNMATCHED,
                null,
                null
        );
    }

    private static String match(
            String value,
            Pattern pattern,
            String name
    ) {
        String normalized = required(value, name);
        if (!pattern.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    name + " does not match the approved contract format"
            );
        }
        return normalized;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
