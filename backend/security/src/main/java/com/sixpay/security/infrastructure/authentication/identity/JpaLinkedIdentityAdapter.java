package com.sixpay.security.infrastructure.authentication.identity;

import com.sixpay.security.application.port.out.FindLinkedIdentityPort;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.LinkedUserIdentity;
import com.sixpay.security.domain.authentication.SixpayUserAccount;
import com.sixpay.security.domain.authentication.UserIdentity;

import java.util.Objects;
import java.util.Optional;

public final class JpaLinkedIdentityAdapter
        implements FindLinkedIdentityPort {

    private final SecurityUserIdentitySpringDataRepository repository;

    public JpaLinkedIdentityAdapter(
            SecurityUserIdentitySpringDataRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<LinkedUserIdentity> findLinkedIdentity(
            AuthenticationIdentityType identityType,
            String provider,
            String providerSubject
    ) {
        return repository
                .findLinkedIdentity(
                        identityType,
                        provider,
                        providerSubject
                )
                .map(JpaLinkedIdentityAdapter::toDomain);
    }

    private static LinkedUserIdentity toDomain(
            SecurityUserIdentityJpaEntity entity
    ) {
        SecurityUserAccountJpaEntity accountEntity =
                entity.getUserAccount();

        SixpayUserAccount account =
                accountEntity.toDomain();

        UserIdentity identity =
                new UserIdentity(
                        entity.getId(),
                        accountEntity.getId(),
                        entity.getIdentityType(),
                        entity.getProvider(),
                        entity.getProviderSubject(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                );

        return new LinkedUserIdentity(
                account,
                identity
        );
    }
}
