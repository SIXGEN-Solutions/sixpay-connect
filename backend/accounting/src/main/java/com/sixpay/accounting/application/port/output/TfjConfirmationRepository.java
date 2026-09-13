package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.TfjConfirmation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TfjConfirmationRepository {
    Optional<TfjConfirmation> findByConfirmationId(UUID confirmationId);
    Optional<TfjConfirmation> findByIdempotencyKey(String idempotencyKey);

    OperationalPage searchOperational(
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference,
            int page,
            int size
    );

    List<TfjConfirmation> searchOperationalAll(
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference
    );
    TfjConfirmation save(TfjConfirmation confirmation);
    List<TfjConfirmation> findPendingFinalityPublication(int limit);

    record OperationalPage(
            List<TfjConfirmation> content,
            long totalElements
    ) {
    }
}
