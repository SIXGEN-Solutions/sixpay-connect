package com.sixpay.security.local;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalAuthUserRepository
        extends JpaRepository<LocalAuthUserEntity, UUID> {

    Optional<LocalAuthUserEntity> findByUsernameIgnoreCase(String username);
}
