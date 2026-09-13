package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.AccountingT1OperationalCandidateNotFoundException;
import com.sixpay.accounting.application.port.input.AccountingT1OperationalQueryUseCase;
import com.sixpay.accounting.domain.model.AccountingT1EligibilityReason;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.AccountingT1OperationalSnapshot;
import com.sixpay.accounting.domain.model.AccountingT1TechnicalIssue;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountingT1OperationalQueryController.class)
@ContextConfiguration(classes = {
        AccountingT1OperationalQueryController.class,
        AccountingT1OperationalQueryExceptionHandler.class,
        AccountingT1OperationalQueryControllerTest.MethodSecurityConfiguration.class
})
class AccountingT1OperationalQueryControllerTest {

    private static final UUID CANDIDATE_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PAYMENT_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountingT1OperationalQueryUseCase query;

    @Test
    @WithMockUser(
            username = "auditor@sixpay",
            authorities = {"ROLE_AUDITOR", "SCOPE_accounting.read"}
    )
    void searchesOperationalCandidatesWithApprovedFilters() throws Exception {
        when(query.search(
                LocalDate.of(2026, 9, 11),
                AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH,
                "PAY-001",
                0,
                20
        )).thenReturn(new AccountingT1OperationalQueryUseCase.Page(
                List.of(snapshot()),
                0,
                20,
                1
        ));

        mockMvc.perform(
                        get("/internal/api/v1/accounting-t1-operations")
                                .queryParam("businessDate", "2026-09-11")
                                .queryParam("status", "ELIGIBLE_FOR_BATCH")
                                .queryParam("paymentReference", "PAY-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].candidateId")
                        .value(CANDIDATE_ID.toString()))
                .andExpect(jsonPath("$.content[0].status")
                        .value("ELIGIBLE_FOR_BATCH"))
                .andExpect(jsonPath("$.content[0].eligibilityReason")
                        .value("NONE"))
                .andExpect(jsonPath("$.content[0].technicalIssue")
                        .value("NONE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(query).search(
                LocalDate.of(2026, 9, 11),
                AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH,
                "PAY-001",
                0,
                20
        );
    }

    @Test
    @WithMockUser(
            username = "manager@sixpay",
            authorities = {"ROLE_MANAGER", "SCOPE_accounting.read"}
    )
    void readsOperationalCandidateDetail() throws Exception {
        when(query.findByCandidateId(CANDIDATE_ID))
                .thenReturn(snapshot());

        mockMvc.perform(
                        get(
                                "/internal/api/v1/accounting-t1-operations/{candidateId}",
                                CANDIDATE_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId")
                        .value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.tresorPayProviderStatus")
                        .value("COMPLETED"));
    }

    @Test
    @WithMockUser(
            username = "auditor-without-scope@sixpay",
            authorities = "ROLE_AUDITOR"
    )
    void rejectsAuthorizedRoleWithoutAccountingReadScope() throws Exception {
        mockMvc.perform(get("/internal/api/v1/accounting-t1-operations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "partner@sixpay",
            authorities = {"ROLE_PARTNER", "SCOPE_accounting.read"}
    )
    void rejectsReadScopeWithoutApprovedOperatorRole() throws Exception {
        mockMvc.perform(get("/internal/api/v1/accounting-t1-operations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnauthenticatedOperationalQuery() throws Exception {
        mockMvc.perform(get("/internal/api/v1/accounting-t1-operations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(
            username = "admin@sixpay",
            authorities = {"ROLE_ADMIN", "SCOPE_accounting.read"}
    )
    void returns404ForUnknownOperationalCandidate() throws Exception {
        when(query.findByCandidateId(any()))
                .thenThrow(new AccountingT1OperationalCandidateNotFoundException(CANDIDATE_ID));

        mockMvc.perform(
                        get(
                                "/internal/api/v1/accounting-t1-operations/{candidateId}",
                                CANDIDATE_ID
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("Accounting T1 operational candidate not found"));
    }

    private static AccountingT1OperationalSnapshot snapshot() {
        return new AccountingT1OperationalSnapshot(
                CANDIDATE_ID,
                PAYMENT_ID,
                "PAY-001",
                "LAREGIONALE",
                LocalDate.of(2026, 9, 11),
                AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH,
                "COMPLETED",
                Instant.parse("2026-09-11T10:01:00Z"),
                AccountingT1EligibilityReason.NONE,
                LocalDate.of(2026, 9, 11),
                Instant.parse("2026-09-11T00:00:00Z"),
                Instant.parse("2026-09-12T00:00:00Z"),
                AccountingT1TechnicalIssue.NONE,
                null
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
