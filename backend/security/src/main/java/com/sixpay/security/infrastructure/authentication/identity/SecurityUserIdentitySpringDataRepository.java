package com.sixpay.security.infrastructure.authentication.identity;

import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SecurityUserIdentitySpringDataRepository
        extends JpaRepository<SecurityUserIdentityJpaEntity, UUID> {

    @Query("""
            select identity
            from SecurityUserIdentityJpaEntity identity
            join fetch identity.userAccount account
            where identity.identityType = :identityType
              and identity.provider = :provider
              and identity.providerSubject = :providerSubject
            """)
    Optional<SecurityUserIdentityJpaEntity> findLinkedIdentity(
            @Param("identityType") AuthenticationIdentityType identityType,
            @Param("provider") String provider,
            @Param("providerSubject") String providerSubject
    );
}
