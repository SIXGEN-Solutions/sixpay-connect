package com.sixpay.security.local;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.security.local",
        name = "seed-enabled",
        havingValue = "true"
)
public class LocalAuthSeeder implements ApplicationRunner {

    private final LocalAuthUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final LocalAuthSeedProperties properties;

    public LocalAuthSeeder(
            LocalAuthUserRepository repository,
            PasswordEncoder passwordEncoder,
            LocalAuthSeedProperties properties
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(
                "admin",
                properties.adminPassword(),
                "local-admin",
                LocalRole.ADMIN
        );
        seed(
                "manager",
                properties.managerPassword(),
                "local-manager",
                LocalRole.MANAGER
        );
        seed(
                "auditor",
                properties.auditorPassword(),
                "local-auditor",
                LocalRole.AUDITOR
        );
        seed(
                "partner-demo",
                properties.partnerPassword(),
                properties.partnerSubject(),
                LocalRole.PARTNER
        );
    }

    private void seed(
            String username,
            String rawPassword,
            String subject,
            LocalRole role
    ) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException(
                    "Missing local authentication seed password for " + username
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException(
                    "Missing local authentication subject for " + username
            );
        }

        if (repository.findByUsernameIgnoreCase(username).isPresent()) {
            return;
        }

        var now = Instant.now();
        repository.save(
                new LocalAuthUserEntity(
                        UUID.randomUUID(),
                        username,
                        passwordEncoder.encode(rawPassword),
                        subject,
                        true,
                        Set.of(role),
                        now,
                        now
                )
        );
    }
}
