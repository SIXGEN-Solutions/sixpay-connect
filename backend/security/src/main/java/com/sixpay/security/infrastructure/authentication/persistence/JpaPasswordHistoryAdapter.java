package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.port.output.PasswordHistoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class JpaPasswordHistoryAdapter implements PasswordHistoryPort {
    private final LocalAuthenticationUserSpringDataRepository localRepository;
    private final PasswordHistorySpringDataRepository historyRepository;

    public JpaPasswordHistoryAdapter(
            LocalAuthenticationUserSpringDataRepository localRepository,
            PasswordHistorySpringDataRepository historyRepository
    ) {
        this.localRepository = Objects.requireNonNull(localRepository);
        this.historyRepository = Objects.requireNonNull(historyRepository);
    }

    @Override
    public PasswordHistorySnapshot loadForPasswordReplacement(UUID userId, int historySize) {
        if (historySize < 0) {
            throw new IllegalArgumentException("Password history size must not be negative");
        }
        LocalAuthenticationUserJpaEntity local = localRepository
                .findForCredentialUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Local authentication is not provisioned for user"
                ));

        List<String> history = historySize == 0
                ? List.of()
                : historyRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(historySize)
                .map(PasswordHistoryJpaEntity::getPasswordHash)
                .toList();

        return new PasswordHistorySnapshot(local.getPasswordHash(), history);
    }

    @Override
    public void archiveReplacedPassword(
            UUID userId,
            String replacedPasswordHash,
            Instant createdAt,
            int historySize
    ) {
        if (historySize < 0) {
            throw new IllegalArgumentException("Password history size must not be negative");
        }
        historyRepository.save(
                PasswordHistoryJpaEntity.archived(userId, replacedPasswordHash, createdAt)
        );
        List<PasswordHistoryJpaEntity> ordered =
                historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (ordered.size() > historySize) {
            historyRepository.deleteAll(ordered.subList(historySize, ordered.size()));
        }
    }
}
