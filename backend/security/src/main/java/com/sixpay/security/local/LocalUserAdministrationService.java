package com.sixpay.security.local;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalUserAdministrationService {

    private final LocalAuthUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public LocalUserAdministrationService(
            LocalAuthUserRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LocalAuthUserEntity create(
            String username,
            String rawPassword,
            String subject,
            Set<LocalRole> roles
    ) {
        if (repository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new IllegalArgumentException(
                    "Local authentication username already exists"
            );
        }

        var now = Instant.now();
        return repository.save(
                new LocalAuthUserEntity(
                        UUID.randomUUID(),
                        username.trim(),
                        passwordEncoder.encode(rawPassword),
                        subject.trim(),
                        true,
                        roles,
                        now,
                        now
                )
        );
    }
}
