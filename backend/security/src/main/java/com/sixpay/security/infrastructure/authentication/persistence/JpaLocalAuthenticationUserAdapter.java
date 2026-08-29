package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.port.output.ChangeLocalCredentialPort;
import com.sixpay.security.application.port.output.LoadAuthenticationUserPort;
import com.sixpay.security.application.port.output.SaveAuthenticationUserStatePort;
import com.sixpay.security.domain.authentication.LocalAuthenticationUser;
import com.sixpay.security.domain.authentication.LocalCredential;
import com.sixpay.security.domain.authentication.PasswordPolicy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JpaLocalAuthenticationUserAdapter
        implements LoadAuthenticationUserPort,
        SaveAuthenticationUserStatePort,
        ChangeLocalCredentialPort {

    private final LocalAuthenticationUserSpringDataRepository repository;
    private final TimeProvider timeProvider;

    public JpaLocalAuthenticationUserAdapter(
            LocalAuthenticationUserSpringDataRepository repository,
            TimeProvider timeProvider
    ) {
        this.repository =
                Objects.requireNonNull(repository);
        this.timeProvider =
                Objects.requireNonNull(timeProvider);
    }

    @Override
    public Optional<LocalAuthenticationUser>
    loadForAuthentication(
            String normalizedUsername
    ) {
        return repository
                .findForAuthentication(
                        normalizedUsername
                )
                .map(
                        JpaLocalAuthenticationUserAdapter::toDomain
                );
    }

    @Override
    public void saveAuthenticationState(
            LocalAuthenticationUser user
    ) {
        LocalAuthenticationUserJpaEntity entity =
                repository.findById(
                        user.id()
                )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Local authentication user disappeared"
                                )
                        );

        entity.updateAuthenticationState(
                user.failedAttempts(),
                user.lockedUntil(),
                user.lastAuthenticatedAt(),
                timeProvider.now()
        );

        repository.save(entity);
    }

    @Override
    public void changePassword(
            UUID userId,
            String passwordHash,
            Instant changedAt,
            PasswordPolicy passwordPolicy
    ) {
        LocalAuthenticationUserJpaEntity entity =
                repository
                        .findForCredentialUpdate(
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Local authentication is not provisioned for user"
                                )
                        );

        entity.changePassword(
                passwordHash,
                changedAt,
                passwordPolicy
        );

        repository.save(entity);
    }

    private static LocalAuthenticationUser toDomain(
            LocalAuthenticationUserJpaEntity entity
    ) {
        var account =
                entity.getUserAccount();

        LocalCredential credential =
                new LocalCredential(
                        account.getId(),
                        entity.getPasswordHash(),
                        entity.isMustChangePassword(),
                        entity.getPasswordChangedAt(),
                        entity.getExpiresAt(),
                        entity.getCredentialUpdatedAt()
                );

        return new LocalAuthenticationUser(
                entity.getId(),
                account.getId(),
                entity.getSubject(),
                account.getUsername(),
                credential,
                entity.getStatus(),
                account.getStatus(),
                account.getAuthorities(),
                entity.getFailedAttempts(),
                entity.getLockedUntil(),
                entity.getLastAuthenticatedAt()
        );
    }
}
