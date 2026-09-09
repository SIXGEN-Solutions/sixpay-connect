package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.TfjConfirmation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TfjConfirmationRepository {
    Optional<TfjConfirmation> findByConfirmationId(UUID confirmationId);
    Optional<TfjConfirmation> findByIdempotencyKey(String idempotencyKey);
    TfjConfirmation save(TfjConfirmation confirmation);
    List<TfjConfirmation> findPendingFinalityPublication(int limit);
}
