package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.port.out.LoadAuthenticationUserPort;
import com.sixpay.security.application.port.out.SaveAuthenticationUserStatePort;
import com.sixpay.security.domain.authentication.LocalAuthenticationUser;
import com.sixpay.security.domain.authentication.LocalCredential;

import java.util.Objects;
import java.util.Optional;

public final class JpaLocalAuthenticationUserAdapter
        implements LoadAuthenticationUserPort, SaveAuthenticationUserStatePort {

    private final LocalAuthenticationUserSpringDataRepository repository;
    private final TimeProvider timeProvider;

    public JpaLocalAuthenticationUserAdapter(
            LocalAuthenticationUserSpringDataRepository repository,
            TimeProvider timeProvider
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public Optional<LocalAuthenticationUser> loadForAuthentication(String normalizedUsername) {
        return repository.findForAuthentication(normalizedUsername)
                .map(JpaLocalAuthenticationUserAdapter::toDomain);
    }

    @Override
    public void saveAuthenticationState(LocalAuthenticationUser user) {
        LocalAuthenticationUserJpaEntity entity = repository.findById(user.id())
                .orElseThrow(() -> new IllegalStateException("Local authentication user disappeared"));

        entity.updateAuthenticationState(
                user.failedAttempts(),
                user.lockedUntil(),
                user.lastAuthenticatedAt(),
                timeProvider.now()
        );
        repository.save(entity);
    }

    private static LocalAuthenticationUser toDomain(LocalAuthenticationUserJpaEntity entity) {
        var account = entity.getUserAccount();
        LocalCredential credential = new LocalCredential(
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
