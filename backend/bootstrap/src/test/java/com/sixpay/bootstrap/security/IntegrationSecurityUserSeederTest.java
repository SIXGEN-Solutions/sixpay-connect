package com.sixpay.bootstrap.security;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.authorization.SixpayPermission;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntegrationSecurityUserSeederTest {

    private static final String ADMIN_PASSWORD =
            "admin-dev-2026";
    private static final String MANAGER_PASSWORD =
            "manager-dev-2026";
    private static final String AUDITOR_PASSWORD =
            "auditor-dev-2026";
    private static final String PARTNER_PASSWORD =
            "partner-dev-2026";

    private static final UUID PARTNER_SUBJECT =
            UUID.fromString(
                    "f88166d1-39df-4900-bb31-1700d25c3bfa"
            );

    @Test
    void seedsFourProfilesWithExpectedRolesAndPermissions() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        when(useCase.listUsers())
                .thenReturn(List.of());

        seeder(useCase)
                .run(
                        new DefaultApplicationArguments(
                                new String[0]
                        )
                );

        ArgumentCaptor<CreateSecurityUserCommand> commands =
                ArgumentCaptor.forClass(
                        CreateSecurityUserCommand.class
                );

        verify(useCase, times(4))
                .createUser(commands.capture());

        List<CreateSecurityUserCommand> values =
                commands.getAllValues();

        assertThat(values)
                .extracting(
                        CreateSecurityUserCommand::username
                )
                .containsExactly(
                        "admin",
                        "manager",
                        "auditor",
                        "partner"
                );

        assertThat(values.get(0).roles())
                .containsExactly("ADMIN");
        assertThat(values.get(0).permissions())
                .containsExactlyInAnyOrderElementsOf(
                        SixpayPermission.valuesAsSet()
                );

        assertThat(values.get(1).roles())
                .containsExactly("MANAGER");
        assertThat(values.get(1).permissions())
                .containsExactlyInAnyOrder(
                        "observed-customer.read",
                        "payment.read",
                        "payment.write",
                        "payment.audit",
                        "payment.reverse"
                );

        assertThat(values.get(2).roles())
                .containsExactly("AUDITOR");
        assertThat(values.get(2).permissions())
                .containsExactlyInAnyOrder(
                        "observed-customer.read",
                        "payment.read",
                        "payment.audit",
                        "payment.audit.read",
                        "payment.audit.export"
                );

        assertThat(values.get(3).userId())
                .isEqualTo(PARTNER_SUBJECT);
        assertThat(values.get(3).roles())
                .containsExactly("PARTNER");
        assertThat(values.get(3).permissions())
                .containsExactly("payment.read");

        assertThat(values)
                .allSatisfy(command -> {
                    assertThat(
                            command.localAuthenticationEnabled()
                    ).isTrue();

                    assertThat(command.actorSubject())
                            .isEqualTo(
                                    IntegrationSecurityUserSeeder
                                            .ACTOR
                            );
                });
    }

    @Test
    void isIdempotentWhenExistingProfilesAlreadyConform() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        UUID adminId =
                IntegrationSecurityUserSeeder
                        .deterministicId("admin");

        UUID managerId =
                IntegrationSecurityUserSeeder
                        .deterministicId("manager");

        UUID auditorId =
                IntegrationSecurityUserSeeder
                        .deterministicId("auditor");

        when(useCase.listUsers())
                .thenReturn(
                        List.of(
                                summary(adminId, "admin"),
                                summary(managerId, "manager"),
                                summary(auditorId, "auditor"),
                                summary(
                                        PARTNER_SUBJECT,
                                        "partner"
                                )
                        )
                );

        when(useCase.getUser(adminId))
                .thenReturn(
                        detail(
                                adminId,
                                "admin",
                                Set.of("ADMIN"),
                                SixpayPermission.valuesAsSet()
                        )
                );

        when(useCase.getUser(managerId))
                .thenReturn(
                        detail(
                                managerId,
                                "manager",
                                Set.of("MANAGER"),
                                Set.of(
                                        "observed-customer.read",
                                        "payment.read",
                                        "payment.write",
                                        "payment.audit",
                                        "payment.reverse"
                                )
                        )
                );

        when(useCase.getUser(auditorId))
                .thenReturn(
                        detail(
                                auditorId,
                                "auditor",
                                Set.of("AUDITOR"),
                                Set.of(
                                        "observed-customer.read",
                                        "payment.read",
                                        "payment.audit",
                                        "payment.audit.read",
                                        "payment.audit.export"
                                )
                        )
                );

        when(useCase.getUser(PARTNER_SUBJECT))
                .thenReturn(
                        detail(
                                PARTNER_SUBJECT,
                                "partner",
                                Set.of("PARTNER"),
                                Set.of("payment.read")
                        )
                );

        seeder(useCase)
                .run(
                        new DefaultApplicationArguments(
                                new String[0]
                        )
                );

        verify(useCase, never())
                .createUser(any());

        verify(useCase, never())
                .updateUser(any());
    }

    @Test
    void reconcilesExistingSeedUserWithMissingPermissions() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        UUID adminId =
                IntegrationSecurityUserSeeder
                        .deterministicId("admin");

        when(useCase.listUsers())
                .thenReturn(
                        List.of(
                                summary(
                                        adminId,
                                        "admin"
                                )
                        )
                );

        when(useCase.getUser(adminId))
                .thenReturn(
                        detail(
                                adminId,
                                "admin",
                                Set.of("ADMIN"),
                                Set.of()
                        )
                );

        seeder(useCase)
                .run(
                        new DefaultApplicationArguments(
                                new String[0]
                        )
                );

        ArgumentCaptor<UpdateSecurityUserCommand> update =
                ArgumentCaptor.forClass(
                        UpdateSecurityUserCommand.class
                );

        verify(useCase)
                .updateUser(update.capture());

        assertThat(update.getValue().userId())
                .isEqualTo(adminId);

        assertThat(update.getValue().roles())
                .containsExactly("ADMIN");

        assertThat(update.getValue().permissions())
                .containsExactlyInAnyOrderElementsOf(
                        SixpayPermission.valuesAsSet()
                );

        verify(useCase, times(3))
                .createUser(any());
    }

    @Test
    void failsClosedWhenSeedUsernameUsesUnexpectedCanonicalId() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        when(useCase.listUsers())
                .thenReturn(
                        List.of(
                                summary(
                                        UUID.randomUUID(),
                                        "admin"
                                )
                        )
                );

        assertThatThrownBy(() ->
                seeder(useCase)
                        .run(
                                new DefaultApplicationArguments(
                                        new String[0]
                                )
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "unexpected canonical id"
                );
    }

    @Test
    void deterministicIdsAreStableAndCaseInsensitive() {
        assertThat(
                IntegrationSecurityUserSeeder
                        .deterministicId("admin")
        ).isEqualTo(
                IntegrationSecurityUserSeeder
                        .deterministicId(" ADMIN ")
        );
    }

    private static IntegrationSecurityUserSeeder seeder(
            SecurityUserAdministrationUseCase useCase
    ) {
        return new IntegrationSecurityUserSeeder(
                useCase,
                ADMIN_PASSWORD,
                MANAGER_PASSWORD,
                AUDITOR_PASSWORD,
                PARTNER_PASSWORD,
                PARTNER_SUBJECT
        );
    }

    private static SecurityUserSummary summary(
            UUID id,
            String username
    ) {
        return new SecurityUserSummary(
                id,
                username,
                username.toLowerCase()
                        + "@sixpay.local",
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                null
        );
    }

    private static SecurityUserDetail detail(
            UUID id,
            String username,
            Set<String> roles,
            Set<String> permissions
    ) {
        return new SecurityUserDetail(
                id,
                username,
                username.toLowerCase()
                        + "@sixpay.local",
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                roles,
                permissions,
                List.of(),
                List.of()
        );
    }
}
