package com.sixpay.partner.api;

import com.sixpay.partner.api.security.PartnerAccessPolicy;
import com.sixpay.partner.application.port.in.PartnerManagementUseCase;
import com.sixpay.partner.application.port.in.PartnerQueryUseCase;
import com.sixpay.partner.application.port.output.PartnerOperationMetrics;
import com.sixpay.partner.application.view.PartnerView;
import com.sixpay.partner.domain.model.PartnerStatus;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authentication.SecurityContextCurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartnerController.class)
@ContextConfiguration(classes = {
        PartnerController.class,
        PartnerApiExceptionHandler.class,
        PartnerAccessPolicy.class,
        PartnerControllerTest.MethodSecurityConfiguration.class
})
class PartnerControllerTest {

    private static final UUID PARTNER_ID =
            UUID.fromString("8ec6a427-406f-4f93-b271-cbc819a4c1dd");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerManagementUseCase management;

    @MockitoBean
    private PartnerQueryUseCase query;

    @MockitoBean
    private PartnerOperationMetrics metrics;

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void createsPartnerForAuthorizedAdministrator() throws Exception {
        when(management.create(any())).thenReturn(partnerView(PartnerStatus.PENDING_VALIDATION));

        mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "corr-001")
                        .header("Idempotency-Key", "idem-001")
                        .content("""
                                {
                                  "legalName": "Acme Payments",
                                  "technicalContactName": "Alice Ops",
                                  "technicalContactEmail": "alice.ops@example.com",
                                  "authorizedTransactionTypes": ["PAYMENT"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/partners/" + PARTNER_ID))
                .andExpect(jsonPath("$.status").value("PENDING_VALIDATION"));
    }

    @Test
    @WithMockUser(username = "external-user", roles = "PARTNER")
    void forbidsPartnerCreationForExternalPartner() throws Exception {
        mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idem-002")
                        .content("""
                                {
                                  "legalName": "Acme Payments",
                                  "technicalContactName": "Alice Ops",
                                  "technicalContactEmail": "alice.ops@example.com",
                                  "authorizedTransactionTypes": ["PAYMENT"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnauthenticatedStatusQuery() throws Exception {
        mockMvc.perform(get("/api/v1/partners/{partnerId}/status", PARTNER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsInvalidCreateRequestWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idem-invalid-001")
                        .content("""
                                {
                                  "legalName": "",
                                  "technicalContactName": "Alice Ops",
                                  "technicalContactEmail": "not-an-email",
                                  "authorizedTransactionTypes": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.legalName").exists())
                .andExpect(jsonPath("$.errors.technicalContactEmail").exists());
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsBlankIdempotencyKeyBeforeCallingTheUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", " ")
                        .content("""
                            {
                              "legalName": "Acme Payments",
                              "technicalContactName": "Alice Ops",
                              "technicalContactEmail": "alice.ops@example.com",
                              "authorizedTransactionTypes": ["PAYMENT"]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));

        verifyNoInteractions(management);
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsHeadersThatExceedThePersistenceContract() throws Exception {
        mockMvc.perform(post("/api/v1/partners")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "c".repeat(151))
                        .header("Idempotency-Key", "i".repeat(151))
                        .content("""
                            {
                              "legalName": "Acme Payments",
                              "technicalContactName": "Alice Ops",
                              "technicalContactEmail": "alice.ops@example.com",
                              "authorizedTransactionTypes": ["PAYMENT"]
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));

        verifyNoInteractions(management);
    }

    @Test
    @WithMockUser(
            username = "8ec6a427-406f-4f93-b271-cbc819a4c1dd",
            roles = "PARTNER"
    )
    void letsPartnerReadOnlyItsOwnStatus() throws Exception {
        when(query.findById(any())).thenReturn(partnerView(PartnerStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/partners/{partnerId}/status", PARTNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerId").value(PARTNER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(
            username = "978cfc85-f3ce-4bca-b02e-1bb915178d9d",
            roles = "PARTNER"
    )
    void forbidsPartnerFromReadingAnotherPartnerStatus() throws Exception {
        mockMvc.perform(get("/api/v1/partners/{partnerId}/status", PARTNER_ID))
                .andExpect(status().isForbidden());
    }

    private static PartnerView partnerView(PartnerStatus status) {
        var now = Instant.parse("2026-07-26T12:00:00Z");
        return new PartnerView(
                PARTNER_ID,
                "Acme Payments",
                "Alice Ops",
                "alice.ops@example.com",
                Set.of("PAYMENT"),
                status,
                null,
                List.of(),
                now,
                now
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new SecurityContextCurrentUserProvider();
        }
    }
}
