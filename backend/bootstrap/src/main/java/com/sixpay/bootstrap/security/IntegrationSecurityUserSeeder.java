package com.sixpay.bootstrap.security;

import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.authorization.SixpayRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("integration")
@ConditionalOnProperty(
        prefix = "sixpay.security.local",
        name = "seed-enabled",
        havingValue = "true"
)
public class IntegrationSecurityUserSeeder
        implements ApplicationRunner {

    static final String ACTOR = "integration-security-seed";

    private final SecurityUserAdministrationUseCase useCase;
    private final String adminPassword;
    private final String managerPassword;
    private final String auditorPassword;
    private final String partnerPassword;
    private final UUID partnerSubject;

    public IntegrationSecurityUserSeeder(
            SecurityUserAdministrationUseCase useCase,
            @Value("${sixpay.security.local.seed.admin-password}") String adminPassword,
            @Value("${sixpay.security.local.seed.manager-password}") String managerPassword,
            @Value("${sixpay.security.local.seed.auditor-password}") String auditorPassword,
            @Value("${sixpay.security.local.seed.partner-password}") String partnerPassword,
            @Value("${sixpay.security.local.seed.partner-subject}") UUID partnerSubject
    ) {
        this.useCase = useCase;
        this.adminPassword = adminPassword;
        this.managerPassword = managerPassword;
        this.auditorPassword = auditorPassword;
        this.partnerPassword = partnerPassword;
        this.partnerSubject = partnerSubject;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> existingUsernames = new HashSet<>(
                useCase.listUsers()
                        .stream()
                        .map(SecurityUserSummary::username)
                        .map(IntegrationSecurityUserSeeder::normalize)
                        .toList()
        );

        seedIfMissing(
                existingUsernames,
                deterministicId("admin"),
                "admin",
                "admin@sixpay.local",
                SixpayRole.ADMIN,
                adminPassword
        );

        seedIfMissing(
                existingUsernames,
                deterministicId("manager"),
                "manager",
                "manager@sixpay.local",
                SixpayRole.MANAGER,
                managerPassword
        );

        seedIfMissing(
                existingUsernames,
                deterministicId("auditor"),
                "auditor",
                "auditor@sixpay.local",
                SixpayRole.AUDITOR,
                auditorPassword
        );

        seedIfMissing(
                existingUsernames,
                partnerSubject,
                "partner",
                "partner@sixpay.local",
                SixpayRole.PARTNER,
                partnerPassword
        );
    }

    private void seedIfMissing(
            Set<String> existingUsernames,
            UUID userId,
            String username,
            String email,
            SixpayRole role,
            String password
    ) {
        String normalizedUsername = normalize(username);

        if (existingUsernames.contains(normalizedUsername)) {
            return;
        }

        useCase.createUser(new CreateSecurityUserCommand(
                userId,
                username,
                email,
                Set.of(role.name()),
                Set.of(),
                true,
                password,
                ACTOR
        ));

        existingUsernames.add(normalizedUsername);
    }

    static UUID deterministicId(String username) {
        return UUID.nameUUIDFromBytes(
                ("sixpay-integration-user:" + normalize(username))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
