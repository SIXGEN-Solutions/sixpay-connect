package com.sixpay.bootstrap.security;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.authorization.SixpayPermission;
import com.sixpay.security.authorization.SixpayRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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

    static final String ACTOR =
            "integration-security-seed";

    private static final Set<String> ADMIN_PERMISSIONS =
            SixpayPermission.valuesAsSet();

    private static final Set<String> MANAGER_PERMISSIONS =
            Set.of(
                    SixpayPermission
                            .OBSERVED_CUSTOMER_READ
                            .value(),
                    SixpayPermission
                            .PAYMENT_READ
                            .value(),
                    SixpayPermission
                            .PAYMENT_WRITE
                            .value(),
                    SixpayPermission
                            .PAYMENT_AUDIT
                            .value(),
                    SixpayPermission
                            .PAYMENT_REVERSE
                            .value()
            );

    private static final Set<String> AUDITOR_PERMISSIONS =
            Set.of(
                    SixpayPermission
                            .OBSERVED_CUSTOMER_READ
                            .value(),
                    SixpayPermission
                            .PAYMENT_READ
                            .value(),
                    SixpayPermission
                            .PAYMENT_AUDIT
                            .value(),
                    SixpayPermission
                            .PAYMENT_AUDIT_READ
                            .value(),
                    SixpayPermission
                            .PAYMENT_AUDIT_EXPORT
                            .value()
            );

    private static final Set<String> PARTNER_PERMISSIONS =
            Set.of(
                    SixpayPermission
                            .PAYMENT_READ
                            .value()
            );

    private final SecurityUserAdministrationUseCase useCase;
    private final String adminPassword;
    private final String managerPassword;
    private final String auditorPassword;
    private final String partnerPassword;
    private final UUID partnerSubject;

    public IntegrationSecurityUserSeeder(
            SecurityUserAdministrationUseCase useCase,
            @Value("${sixpay.security.local.seed.admin-password}")
            String adminPassword,
            @Value("${sixpay.security.local.seed.manager-password}")
            String managerPassword,
            @Value("${sixpay.security.local.seed.auditor-password}")
            String auditorPassword,
            @Value("${sixpay.security.local.seed.partner-password}")
            String partnerPassword,
            @Value("${sixpay.security.local.seed.partner-subject}")
            UUID partnerSubject
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
        Map<String, SecurityUserSummary> existingUsers =
                new LinkedHashMap<>();

        useCase.listUsers()
                .forEach(user ->
                        existingUsers.put(
                                normalize(user.username()),
                                user
                        )
                );

        reconcile(
                existingUsers,
                new SeedProfile(
                        deterministicId("admin"),
                        "admin",
                        "admin@sixpay.local",
                        SixpayRole.ADMIN,
                        ADMIN_PERMISSIONS,
                        adminPassword
                )
        );

        reconcile(
                existingUsers,
                new SeedProfile(
                        deterministicId("manager"),
                        "manager",
                        "manager@sixpay.local",
                        SixpayRole.MANAGER,
                        MANAGER_PERMISSIONS,
                        managerPassword
                )
        );

        reconcile(
                existingUsers,
                new SeedProfile(
                        deterministicId("auditor"),
                        "auditor",
                        "auditor@sixpay.local",
                        SixpayRole.AUDITOR,
                        AUDITOR_PERMISSIONS,
                        auditorPassword
                )
        );

        reconcile(
                existingUsers,
                new SeedProfile(
                        partnerSubject,
                        "partner",
                        "partner@sixpay.local",
                        SixpayRole.PARTNER,
                        PARTNER_PERMISSIONS,
                        partnerPassword
                )
        );
    }

    private void reconcile(
            Map<String, SecurityUserSummary> existingUsers,
            SeedProfile profile
    ) {
        String normalizedUsername =
                normalize(profile.username());

        SecurityUserSummary existing =
                existingUsers.get(normalizedUsername);

        if (existing == null) {
            create(profile);
            return;
        }

        if (!profile.userId().equals(existing.id())) {
            throw new IllegalStateException(
                    "Integration seed user "
                            + profile.username()
                            + " exists with unexpected canonical id "
                            + existing.id()
                            + "; expected "
                            + profile.userId()
            );
        }

        SecurityUserDetail current =
                useCase.getUser(existing.id());

        Set<String> expectedRoles =
                Set.of(profile.role().name());

        boolean requiresReconciliation =
                !profile.username()
                        .equals(current.username())
                        || !profile.email()
                        .equals(current.email())
                        || !expectedRoles
                        .equals(current.roles())
                        || !profile.permissions()
                        .equals(current.permissions());

        if (!requiresReconciliation) {
            return;
        }

        useCase.updateUser(
                new UpdateSecurityUserCommand(
                        profile.userId(),
                        profile.username(),
                        profile.email(),
                        expectedRoles,
                        profile.permissions(),
                        ACTOR
                )
        );
    }

    private void create(SeedProfile profile) {
        useCase.createUser(
                new CreateSecurityUserCommand(
                        profile.userId(),
                        profile.username(),
                        profile.email(),
                        Set.of(profile.role().name()),
                        profile.permissions(),
                        true,
                        profile.password(),
                        ACTOR
                )
        );
    }

    static UUID deterministicId(String username) {
        return UUID.nameUUIDFromBytes(
                ("sixpay-integration-user:"
                        + normalize(username))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String normalize(String username) {
        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private record SeedProfile(
            UUID userId,
            String username,
            String email,
            SixpayRole role,
            Set<String> permissions,
            String password
    ) {
    }
}
