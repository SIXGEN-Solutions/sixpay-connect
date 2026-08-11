package com.sixpay.bootstrap.security;

import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntegrationSecurityUserSeederTest {

    private static final String ADMIN_PASSWORD = "admin-dev-2026";
    private static final String MANAGER_PASSWORD = "manager-dev-2026";
    private static final String AUDITOR_PASSWORD = "auditor-dev-2026";
    private static final String PARTNER_PASSWORD = "partner-dev-2026";

    private static final UUID PARTNER_SUBJECT =
            UUID.fromString("f88166d1-39df-4900-bb31-1700d25c3bfa");

    @Test
    void seedsTheFourCanonicalIntegrationProfilesWhenDatabaseIsEmpty() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        when(useCase.listUsers()).thenReturn(List.of());

        IntegrationSecurityUserSeeder seeder = seeder(useCase);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<CreateSecurityUserCommand> commands =
                ArgumentCaptor.forClass(CreateSecurityUserCommand.class);

        verify(useCase, times(4)).createUser(commands.capture());

        assertThat(commands.getAllValues())
                .extracting(CreateSecurityUserCommand::username)
                .containsExactly(
                        "admin",
                        "manager",
                        "auditor",
                        "partner"
                );

        assertThat(commands.getAllValues())
                .allSatisfy(command -> {
                    assertThat(command.localAuthenticationEnabled()).isTrue();
                    assertThat(command.permissions()).isEmpty();
                    assertThat(command.actorSubject())
                            .isEqualTo(IntegrationSecurityUserSeeder.ACTOR);
                });

        CreateSecurityUserCommand admin = commands.getAllValues().get(0);
        assertThat(admin.userId())
                .isEqualTo(
                        IntegrationSecurityUserSeeder.deterministicId("admin")
                );
        assertThat(admin.roles()).containsExactly("ADMIN");
        assertThat(admin.initialPassword()).isEqualTo(ADMIN_PASSWORD);

        CreateSecurityUserCommand manager = commands.getAllValues().get(1);
        assertThat(manager.roles()).containsExactly("MANAGER");
        assertThat(manager.initialPassword()).isEqualTo(MANAGER_PASSWORD);

        CreateSecurityUserCommand auditor = commands.getAllValues().get(2);
        assertThat(auditor.roles()).containsExactly("AUDITOR");
        assertThat(auditor.initialPassword()).isEqualTo(AUDITOR_PASSWORD);

        CreateSecurityUserCommand partner = commands.getAllValues().get(3);
        assertThat(partner.userId()).isEqualTo(PARTNER_SUBJECT);
        assertThat(partner.roles()).containsExactly("PARTNER");
        assertThat(partner.initialPassword()).isEqualTo(PARTNER_PASSWORD);
    }

    @Test
    void isIdempotentWhenAllIntegrationUsersAlreadyExist() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        when(useCase.listUsers()).thenReturn(List.of(
                summary("admin"),
                summary("manager"),
                summary("auditor"),
                summary("partner")
        ));

        seeder(useCase)
                .run(new DefaultApplicationArguments(new String[0]));

        verify(useCase, never()).createUser(any());
    }

    @Test
    void createsOnlyMissingIntegrationUsers() {
        SecurityUserAdministrationUseCase useCase =
                mock(SecurityUserAdministrationUseCase.class);

        when(useCase.listUsers()).thenReturn(List.of(
                summary("ADMIN"),
                summary("auditor")
        ));

        seeder(useCase)
                .run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<CreateSecurityUserCommand> commands =
                ArgumentCaptor.forClass(CreateSecurityUserCommand.class);

        verify(useCase, times(2)).createUser(commands.capture());

        assertThat(commands.getAllValues())
                .extracting(CreateSecurityUserCommand::username)
                .containsExactly("manager", "partner");
    }

    @Test
    void deterministicIdsAreStableAndCaseInsensitive() {
        assertThat(
                IntegrationSecurityUserSeeder.deterministicId("admin")
        ).isEqualTo(
                IntegrationSecurityUserSeeder.deterministicId(" ADMIN ")
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

    private static SecurityUserSummary summary(String username) {
        return new SecurityUserSummary(
                UUID.randomUUID(),
                username,
                username.toLowerCase() + "@sixpay.local",
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                null
        );
    }
}
