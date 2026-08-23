package com.sixpay.administration.api;

import com.sixpay.administration.application.port.input.IncidentQueryUseCase;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentQueryController.class)
@ContextConfiguration(
        classes = {
                IncidentQueryController.class,
                IncidentQueryRoleAuthorizationTest
                        .SecurityTestConfiguration.class
        }
)
class IncidentQueryRoleAuthorizationTest {

    private static final String API =
            "/internal/api/v1/incidents";

    private static final String CORRELATION =
            "11111111-1111-4111-8111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentQueryUseCase useCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadIncidents() throws Exception {
        arrangeEmptyPage();
        performRead().andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanReadIncidents() throws Exception {
        arrangeEmptyPage();
        performRead().andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void auditorCanReadIncidents() throws Exception {
        arrangeEmptyPage();
        performRead().andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unrelatedRoleCannotReadIncidents()
            throws Exception {

        performRead().andExpect(status().isForbidden());
    }

    private void arrangeEmptyPage() {
        when(useCase.search(any()))
                .thenReturn(
                        new IncidentSearchPage(
                                List.of(),
                                0,
                                0,
                                0,
                                20,
                                true,
                                true
                        )
                );
    }

    private org.springframework.test.web.servlet.ResultActions
    performRead() throws Exception {
        return mockMvc.perform(
                get(API)
                        .param("page", "0")
                        .param("size", "20")
                        .header(
                                "X-Correlation-ID",
                                CORRELATION
                        )
        );
    }

    @Configuration
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
    }
}
