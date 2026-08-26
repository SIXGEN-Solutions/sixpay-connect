package com.sixpay.partner.application.port.output;

import com.sixpay.partner.domain.model.PartnerId;

import java.time.Instant;
import java.util.Optional;

public interface PartnerIdempotencyStore {

    void lock(String operation, String idempotencyKey);

    Optional<PartnerId> findCompleted(
            String operation,
            String idempotencyKey
    );

    void complete(
            String operation,
            String idempotencyKey,
            PartnerId partnerId,
            Instant completedAt
    );
}
