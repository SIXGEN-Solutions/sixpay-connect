package com.sixpay.administration.api;

import com.sixpay.administration.application.port.input.AdministrationQueryUseCase;
import com.sixpay.administration.domain.model.AdministrationOverview;
import com.sixpay.administration.domain.model.AdministrationSettings;
import com.sixpay.administration.domain.model.IntegrationHealth;
import com.sixpay.administration.domain.model.IntegrationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdministrationQueryController.class)
@ContextConfiguration(
        classes = {
                AdministrationQueryController.class,
                AdministrationQueryControllerTest
                        .SecurityTestConfiguration.class
        }
)
class AdministrationQueryControllerTest {

    private static final String API =
            "/internal/api/v1/administration";

    private static final String CORRELATION_ID =
            "11111111-1111-4111-8111-111111111111";

    private static final Instant NOW =
            Instant.parse("2026-08-23T14:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdministrationQueryUseCase useCase;

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(
                        get(API + "/overview")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void rejectsNonAdminAccess() throws Exception {
        mockMvc.perform(
                        get(API + "/overview")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsOverview() throws Exception {
        when(useCase.overview())
                .thenReturn(
                        overview()
                );

        mockMvc.perform(
                        get(API + "/overview")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.settings.accountingCutoffZone"
                        ).value("Africa/Douala")
                )
                .andExpect(
                        jsonPath(
                                "$.settings.accountingCutoffTime"
                        ).value("23:00")
                )
                .andExpect(
                        jsonPath(
                                "$.integrations[0].integrationId"
                        ).value("db")
                )
                .andExpect(
                        jsonPath(
                                "$.integrations[0].health"
                        ).value("AVAILABLE")
                )
                .andExpect(
                        jsonPath("$.observedAt")
                                .value(NOW.toString())
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsSettings() throws Exception {
        when(useCase.settings())
                .thenReturn(
                        new AdministrationSettings(
                                "Africa/Douala",
                                "23:00"
                        )
                );

        mockMvc.perform(
                        get(API + "/settings")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.accountingCutoffZone"
                        ).value("Africa/Douala")
                )
                .andExpect(
                        jsonPath(
                                "$.accountingCutoffTime"
                        ).value("23:00")
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsIntegrations() throws Exception {
        when(useCase.integrations())
                .thenReturn(
                        overview().integrations()
                );

        mockMvc.perform(
                        get(API + "/integrations")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].integrationId")
                                .value("db")
                )
                .andExpect(
                        jsonPath("$[0].health")
                                .value("AVAILABLE")
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void requiresCorrelationId() throws Exception {
        mockMvc.perform(
                        get(API + "/settings")
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    private static AdministrationOverview overview() {
        return new AdministrationOverview(
                new AdministrationSettings(
                        "Africa/Douala",
                        "23:00"
                ),
                List.of(
                        new IntegrationStatus(
                                "db",
                                "PostgreSQL",
                                "DATABASE",
                                IntegrationHealth.AVAILABLE,
                                null,
                                null,
                                NOW
                        )
                ),
                NOW
        );
    }

    @Configuration
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
    }
}
