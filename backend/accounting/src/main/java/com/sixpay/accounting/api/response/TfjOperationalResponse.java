package com.sixpay.accounting.api.response;

import com.sixpay.accounting.domain.model.TfjOperationalCategory;
import com.sixpay.accounting.domain.model.TfjOperationalSnapshot;
import com.sixpay.accounting.domain.model.TfjObservationChannel;
import com.sixpay.accounting.domain.model.TfjMatchStatus;
import com.sixpay.accounting.domain.model.TfjRecoveryAction;
import com.sixpay.accounting.domain.model.TfjStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TfjOperationalResponse(
        UUID confirmationId,
        String financialInstitutionCode,
        LocalDate businessDate,
        String paymentReference,
        String bankPostingReference,
        String tfjBatchReference,
        TfjStatus tfjStatus,
        Instant confirmedAt,
        TfjObservationChannel observationChannel,
        String correlationId,
        TfjMatchStatus matchStatus,
        UUID matchedPaymentId,
        Instant finalityPublishedAt,
        String failureCode,
        TfjRecoveryAction recoveryAction,
        TfjOperationalCategory category
) {
    public static TfjOperationalResponse from(
            TfjOperationalSnapshot snapshot
    ) {
        return new TfjOperationalResponse(
                snapshot.confirmationId(),
                snapshot.financialInstitutionCode(),
                snapshot.businessDate(),
                snapshot.paymentReference(),
                snapshot.bankPostingReference(),
                snapshot.tfjBatchReference(),
                snapshot.tfjStatus(),
                snapshot.confirmedAt(),
                snapshot.observationChannel(),
                snapshot.correlationId(),
                snapshot.matchStatus(),
                snapshot.matchedPaymentId(),
                snapshot.finalityPublishedAt(),
                snapshot.failureCode(),
                snapshot.recoveryAction(),
                snapshot.category()
        );
    }
}
