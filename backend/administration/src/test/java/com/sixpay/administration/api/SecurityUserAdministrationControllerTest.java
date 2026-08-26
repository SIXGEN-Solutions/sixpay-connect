package com.sixpay.administration.api;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityUserAdministrationController.class)
@ContextConfiguration(
        classes = {
                SecurityUserAdministrationController.class,
                SecurityUserAdministrationControllerTest
                        .SecurityTestConfiguration.class
        }
)
class SecurityUserAdministrationControllerTest {

    private static final String API =
            "/internal/api/v1/administration/users";

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityUserAdministrationUseCase useCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void rejectsAnonymousAccess() throws Exception {

        mockMvc.perform(
                        get(API)
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(
            username = "auditor",
            roles = "AUDITOR"
    )
    void rejectsNonAdminAccess() throws Exception {

        mockMvc.perform(
                        get(API)
                )
                .andExpect(
                        status().isForbidden()
                );

        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = "ADMIN"
    )
    void listsUsersForAdmin() throws Exception {

        when(useCase.listUsers())
                .thenReturn(
                        List.of(
                                new SecurityUserSummary(
                                        USER_ID,
                                        "admin",
                                        "admin@sixpay.local",
                                        SixpayUserAccountStatus.ACTIVE,
                                        true,
                                        false,
                                        null
                                )
                        )
                );

        mockMvc.perform(
                        get(API)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(
                                        USER_ID.toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].username")
                                .value("admin")
                )
                .andExpect(
                        jsonPath("$[0].localEnabled")
                                .value(true)
                );
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = "ADMIN"
    )
    void createsUserAndPropagatesAuthenticatedActor()
            throws Exception {

        authenticatedActor(
                "admin-subject"
        );

        SecurityUserDetail created =
                detail(
                        USER_ID,
                        "ops-admin",
                        "ops-admin@sixpay.local",
                        Set.of("ADMIN")
                );

        when(
                useCase.createUser(
                        any(CreateSecurityUserCommand.class)
                )
        )
                .thenReturn(created);

        mockMvc.perform(
                        post(API)
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "username": "ops-admin",
                                          "email": "ops-admin@sixpay.local",
                                          "roles": ["ADMIN"],
                                          "permissions": ["payment.read"],
                                          "localAuthenticationEnabled": true,
                                          "initialPassword": "Admin-dev-2026"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        header().string(
                                "Location",
                                "http://localhost"
                                        + API
                                        + "/"
                                        + USER_ID
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        USER_ID.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.username")
                                .value("ops-admin")
                );

        ArgumentCaptor<CreateSecurityUserCommand> command =
                ArgumentCaptor.forClass(
                        CreateSecurityUserCommand.class
                );

        verify(useCase)
                .createUser(
                        command.capture()
                );

        assertThat(
                command.getValue().username()
        )
                .isEqualTo("ops-admin");

        assertThat(
                command.getValue().roles()
        )
                .containsExactly("ADMIN");

        assertThat(
                command.getValue().permissions()
        )
                .containsExactly("payment.read");

        assertThat(
                command.getValue()
                        .localAuthenticationEnabled()
        )
                .isTrue();

        assertThat(
                command.getValue()
                        .actorSubject()
        )
                .isEqualTo(
                        "admin-subject"
                );
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = "ADMIN"
    )
    void rejectsInvalidCreatePayloadBeforeUseCase()
            throws Exception {

        mockMvc.perform(
                        post(API)
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "username": "",
                                          "email": "not-an-email",
                                          "roles": ["ADMIN"],
                                          "permissions": [],
                                          "localAuthenticationEnabled": true,
                                          "initialPassword": "short"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = "ADMIN"
    )
    void updatesCanonicalProfileAndAuthorization()
            throws Exception {

        authenticatedActor(
                "admin-subject"
        );

        when(
                useCase.updateUser(
                        any(UpdateSecurityUserCommand.class)
                )
        )
                .thenReturn(
                        detail(
                                USER_ID,
                                "manager-ops",
                                "manager@sixpay.local",
                                Set.of("MANAGER")
                        )
                );

        mockMvc.perform(
                        put(
                                API
                                        + "/"
                                        + USER_ID
                        )
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "username": "manager-ops",
                                          "email": "manager@sixpay.local",
                                          "roles": ["MANAGER"],
                                          "permissions": ["reporting.read"]
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.username")
                                .value(
                                        "manager-ops"
                                )
                );

        ArgumentCaptor<UpdateSecurityUserCommand> command =
                ArgumentCaptor.forClass(
                        UpdateSecurityUserCommand.class
                );

        verify(useCase)
                .updateUser(
                        command.capture()
                );

        assertThat(
                command.getValue()
                        .userId()
        )
                .isEqualTo(USER_ID);

        assertThat(
                command.getValue()
                        .actorSubject()
        )
                .isEqualTo(
                        "admin-subject"
                );
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = "ADMIN"
    )
    void enablesAndDisablesUser()
            throws Exception {

        authenticatedActor(
                "admin-subject"
        );

        when(
                useCase.enableUser(
                        USER_ID,
                        "admin-subject"
                )
        )
                .thenReturn(
                        detail(
                                USER_ID,
                                "manager",
                                "manager@sixpay.local",
                                Set.of("MANAGER")
                        )
                );

        when(
                useCase.disableUser(
                        USER_ID,
                        "admin-subject"
                )
        )
                .thenReturn(
                        disabledDetail(
                                USER_ID
                        )
                );

        mockMvc.perform(
                        post(
                                API
                                        + "/"
                                        + USER_ID
                                        + "/enable"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );

        mockMvc.perform(
                        post(
                                API
                                        + "/"
                                        + USER_ID
                                        + "/disable"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DISABLED")
                );
    }

    @Test
    @WithMockUser(
            username = "admin",
            roles = "ADMIN"
    )
    void deletesUserAndReturnsNoContent()
            throws Exception {

        authenticatedActor(
                "admin-subject"
        );

        mockMvc.perform(
                        delete(
                                API
                                        + "/"
                                        + USER_ID
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        content().string("")
                );

        verify(useCase)
                .deleteUser(
                        USER_ID,
                        "admin-subject"
                );
    }

    private void authenticatedActor(
            String subject
    ) {

        when(
                currentUserProvider
                        .requireCurrentUser()
        )
                .thenReturn(
                        new AuthenticatedUser(
                                subject,
                                "admin",
                                Set.of(
                                        "ROLE_ADMIN"
                                )
                        )
                );
    }

    private static SecurityUserDetail detail(
            UUID id,
            String username,
            String email,
            Set<String> roles
    ) {

        return new SecurityUserDetail(
                id,
                username,
                email,
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                roles,
                Set.of(),
                List.of(),
                List.of()
        );
    }

    private static SecurityUserDetail disabledDetail(
            UUID id
    ) {

        return new SecurityUserDetail(
                id,
                "manager",
                "manager@sixpay.local",
                SixpayUserAccountStatus.DISABLED,
                true,
                false,
                Set.of(
                        "MANAGER"
                ),
                Set.of(),
                List.of(),
                List.of()
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
    }
}