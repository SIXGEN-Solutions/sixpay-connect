package com.sixpay.security.application.port.output;

import com.sixpay.security.application.model.PasswordHistorySnapshot;
import java.time.Instant;
import java.util.UUID;

public interface PasswordHistoryPort {
    PasswordHistorySnapshot loadForPasswordReplacement(
            UUID userId,
            int historySize
    );

    void archiveReplacedPassword(
            UUID userId,
            String replacedPasswordHash,
            Instant createdAt,
            int historySize
    );
}
