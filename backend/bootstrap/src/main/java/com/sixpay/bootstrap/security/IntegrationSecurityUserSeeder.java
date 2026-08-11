package com.sixpay.bootstrap.security;

import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
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

    private static final String ACTOR = "integration-security-seed";

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
        seed(
                deterministicId("admin"),
                "admin",
                "admin@sixpay.local",
                Set.of("ADMIN"),
                adminPassword
        );
        seed(
                deterministicId("manager"),
                "manager",
                "manager@sixpay.local",
                Set.of("MANAGER"),
                managerPassword
        );
        seed(
                deterministicId("auditor"),
                "auditor",
                "auditor@sixpay.local",
                Set.of("AUDITOR"),
                auditorPassword
        );
        seed(
                partnerSubject,
                "partner",
                "partner@sixpay.local",
                Set.of("PARTNER"),
                partnerPassword
        );
    }

    private void seed(
            UUID userId,
            String username,
            String email,
            Set<String> roles,
            String password
    ) {
        boolean exists = useCase.listUsers()
                .stream()
                .anyMatch(user ->
                        user.username().equalsIgnoreCase(username)
                );

        if (exists) {
            return;
        }

        useCase.createUser(new CreateSecurityUserCommand(
                userId,
                username,
                email,
                roles,
                Set.of(),
                true,
                password,
                ACTOR
        ));
    }

    private static UUID deterministicId(String username) {
        return UUID.nameUUIDFromBytes(
                ("sixpay-integration-user:" + username)
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
