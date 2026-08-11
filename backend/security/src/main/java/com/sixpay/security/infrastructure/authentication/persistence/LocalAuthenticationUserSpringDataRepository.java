package com.sixpay.security.infrastructure.authentication.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LocalAuthenticationUserSpringDataRepository
        extends JpaRepository<LocalAuthenticationUserJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select localUser
            from LocalAuthenticationUserJpaEntity localUser
            join fetch localUser.userAccount account
            where localUser.normalizedUsername = :normalizedUsername
            """)
    Optional<LocalAuthenticationUserJpaEntity> findForAuthentication(
            @Param("normalizedUsername")
            String normalizedUsername
    );
}
