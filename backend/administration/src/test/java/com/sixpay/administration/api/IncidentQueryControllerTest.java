package com.sixpay.administration.api;

import com.sixpay.administration.application.port.input.IncidentQueryUseCase;
import com.sixpay.administration.domain.model.IncidentId;
import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.model.OperationalIncident;
import com.sixpay.administration.domain.repository.IncidentSearchPage;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentQueryController.class)
@ContextConfiguration(
        classes = {
                IncidentQueryController.class,
                IncidentApiExceptionHandler.class,
                IncidentQueryControllerTest
                        .SecurityTestConfiguration.class
        }
)
class IncidentQueryControllerTest {

    private static final String API =
            "/internal/api/v1/incidents";

    private static final String CORRELATION =
            "11111111-1111-4111-8111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentQueryUseCase useCase;

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(
                        get(API)
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION
                                )
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchesIncidents() throws Exception {
        when(useCase.search(any()))
                .thenReturn(
                        new IncidentSearchPage(
                                List.of(incident()),
                                1,
                                1,
                                0,
                                20,
                                true,
                                true
                        )
                );

        mockMvc.perform(
                        get(API)
                                .param(
                                        "severity",
                                        "HIGH"
                                )
                                .param(
                                        "status",
                                        "OPEN"
                                )
                                .param(
                                        "component",
                                        "Accounting"
                                )
                                .param("page", "0")
                                .param("size", "20")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.content[0].incidentId"
                        ).value("INC-001")
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(20)
                );
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void getsIncidentDetail() throws Exception {
        when(
                useCase.get(
                        any(IncidentId.class)
                )
        ).thenReturn(incident());

        mockMvc.perform(
                        get(API + "/INC-001")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.incidentId")
                                .value("INC-001")
                )
                .andExpect(
                        jsonPath("$.component")
                                .value("Accounting")
                );
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsUnauthorizedRole() throws Exception {
        mockMvc.perform(
                        get(API)
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    private static OperationalIncident incident() {
        Instant now =
                Instant.parse(
                        "2026-08-23T14:00:00Z"
                );

        return new OperationalIncident(
                new IncidentId("INC-001"),
                IncidentSeverity.HIGH,
                "Accounting",
                "Accounting batch delayed",
                IncidentStatus.OPEN,
                "Batch processing exceeded normal duration",
                "Accounting finalization delayed",
                null,
                null,
                null,
                null,
                now,
                now,
                List.of()
        );
    }

    @Configuration
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
    }
}
