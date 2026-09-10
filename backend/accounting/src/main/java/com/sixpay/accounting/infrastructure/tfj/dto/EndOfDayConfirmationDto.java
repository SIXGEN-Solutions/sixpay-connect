package com.sixpay.accounting.infrastructure.tfj.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EndOfDayConfirmationDto(
        String schemaVersion,
        UUID confirmationId,
        String financialInstitutionCode,
        LocalDate businessDate,
        String paymentReference,
        String bankPostingReference,
        String tfjBatchReference,
        String tfjStatus,
        Instant confirmedAt,
        Failure failure
) {
    public record Failure(
            String code,
            String description,
            String recoveryAction
    ) {
    }
}
