package com.sixpay.administration.api;

import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.*;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        DirectoryUserAdministrationController.class,
        DirectoryUserApiExceptionHandler.class
})
@ContextConfiguration(classes = {
        DirectoryUserAdministrationController.class,
        DirectoryUserApiExceptionHandler.class,
        DirectoryUserAdministrationControllerTest.SecurityTestConfiguration.class
})
class DirectoryUserAdministrationControllerTest {

    private static final String API =
            "/internal/api/v1/administration/directory-users";

    private static final UUID USER_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DirectoryUserLookupUseCase lookupUseCase;

    @MockitoBean
    private ProvisionLdapSecurityUserUseCase provisioningUseCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void anonymousLookupIsUnauthorized() throws Exception {
        mockMvc.perform(get(API + "/jane.doe"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lookupUseCase, provisioningUseCase);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void nonAdminLookupIsForbidden() throws Exception {
        mockMvc.perform(get(API + "/jane.doe"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lookupUseCase, provisioningUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanLookupDirectoryUserWithoutMutation() throws Exception {
        when(lookupUseCase.lookup(any()))
                .thenReturn(profile(DirectoryAccountStatus.ACTIVE));

        mockMvc.perform(get(API + "/jane.doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jane.doe"))
                .andExpect(jsonPath("$.displayName").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane.doe@example.test"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        verify(lookupUseCase).lookup(any());
        verifyNoInteractions(provisioningUseCase, currentUserProvider);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void lookupNotFoundMapsTo404() throws Exception {
        when(lookupUseCase.lookup(any()))
                .thenThrow(new DirectoryUserNotFoundException());

        mockMvc.perform(get(API + "/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ambiguousLookupMapsTo409() throws Exception {
        when(lookupUseCase.lookup(any()))
                .thenThrow(new DirectoryUserAmbiguousException());

        mockMvc.perform(get(API + "/duplicate"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void directoryUnavailableMapsTo503() throws Exception {
        when(lookupUseCase.lookup(any()))
                .thenThrow(new DirectoryUnavailableException(
                        new IllegalStateException("directory unavailable")
                ));

        mockMvc.perform(get(API + "/jane.doe"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void provisioningSuccessPropagatesActorAndSixpayAuthorities() throws Exception {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(
                        "admin-subject",
                        "admin",
                        Set.of("ROLE_ADMIN")
                ));

        when(provisioningUseCase.provision(any()))
                .thenReturn(createdUser());

        mockMvc.perform(
                        post(API + "/jane.doe/provisioning")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "roles": ["ADMIN"],
                                          "permissions": ["payment.read"]
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                "Location",
                                "http://localhost/internal/api/v1/administration/users/"
                                        + USER_ID
                        )
                );

        ArgumentCaptor<ProvisionLdapSecurityUserCommand> command =
                ArgumentCaptor.forClass(
                        ProvisionLdapSecurityUserCommand.class
                );

        verify(provisioningUseCase).provision(command.capture());

        assertThat(command.getValue().directoryUsername())
                .isEqualTo("jane.doe");
        assertThat(command.getValue().roles())
                .containsExactly("ADMIN");
        assertThat(command.getValue().permissions())
                .containsExactly("payment.read");
        assertThat(command.getValue().actorSubject())
                .isEqualTo("admin-subject");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidProvisioningPayloadIs400BeforeUseCase() throws Exception {
        mockMvc.perform(
                        post(API + "/jane.doe/provisioning")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "roles": [""],
                                          "permissions": []
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(provisioningUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nonProvisionableDirectoryAccountMapsTo422() throws Exception {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(
                        "admin-subject",
                        "admin",
                        Set.of("ROLE_ADMIN")
                ));

        when(provisioningUseCase.provision(any()))
                .thenThrow(
                        new LdapUserNotProvisionableException(
                                DirectoryAccountStatus.LOCKED
                        )
                );

        mockMvc.perform(
                        post(API + "/jane.doe/provisioning")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "roles": [],
                                          "permissions": []
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void alreadyProvisionedIdentityMapsTo409Reason() throws Exception {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(
                        "admin-subject",
                        "admin",
                        Set.of("ROLE_ADMIN")
                ));

        when(provisioningUseCase.provision(any()))
                .thenThrow(
                        new LdapProvisioningConflictException(
                                LdapProvisioningConflictException.Reason.ALREADY_PROVISIONED,
                                "already provisioned"
                        )
                );

        mockMvc.perform(
                        post(API + "/jane.doe/provisioning")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "roles": [],
                                          "permissions": []
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("ALREADY_PROVISIONED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usernameCollisionMapsTo409Reason() throws Exception {
        when(currentUserProvider.requireCurrentUser())
                .thenReturn(new AuthenticatedUser(
                        "admin-subject",
                        "admin",
                        Set.of("ROLE_ADMIN")
                ));

        when(provisioningUseCase.provision(any()))
                .thenThrow(
                        new LdapProvisioningConflictException(
                                LdapProvisioningConflictException.Reason.USERNAME_CONFLICT,
                                "username conflict"
                        )
                );

        mockMvc.perform(
                        post(API + "/jane.doe/provisioning")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "roles": [],
                                          "permissions": []
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("USERNAME_CONFLICT"));
    }

    private static DirectoryUserProfile profile(
            DirectoryAccountStatus status
    ) {
        return new DirectoryUserProfile(
                "jane.doe",
                "Jane Doe",
                "jane.doe@example.test",
                "regionale-ldap",
                "00112233-4455-6677-8899-aabbccddeeff",
                status
        );
    }

    private static SecurityUserDetail createdUser() {
        return new SecurityUserDetail(
                USER_ID,
                "jane.doe",
                "jane.doe@example.test",
                SixpayUserAccountStatus.ACTIVE,
                false,
                false,
                Set.of("ADMIN"),
                Set.of("payment.read"),
                List.of(),
                List.of()
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
    }
}
