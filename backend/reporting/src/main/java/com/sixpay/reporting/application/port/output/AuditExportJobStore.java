package com.sixpay.reporting.application.port.output;

import com.sixpay.reporting.application.query.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditExportJobStore {

    AuditExportAcceptance accept(
            RequestPaymentAuditExportCommand command,
            String fingerprint,
            Instant requestedAt,
            Instant expiresAt
    );

    Optional<AuditExportJobDefinition> find(UUID exportId);

    Optional<AuditExportJobDefinition> claim(UUID exportId);

    List<UUID> findAccepted(int limit);

    void complete(
            UUID exportId,
            long recordCount,
            String checksum,
            URI retrievalUri
    );

    void fail(UUID exportId, String failureCode);

    void expire(Instant now);
}
