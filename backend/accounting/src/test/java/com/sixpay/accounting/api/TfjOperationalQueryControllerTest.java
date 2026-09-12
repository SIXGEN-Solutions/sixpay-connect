package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.TfjOperationalConfirmationNotFoundException;
import com.sixpay.accounting.application.port.input.TfjOperationalQueryUseCase;
import com.sixpay.accounting.domain.model.TfjMatchStatus;
import com.sixpay.accounting.domain.model.TfjObservationChannel;
import com.sixpay.accounting.domain.model.TfjOperationalCategory;
import com.sixpay.accounting.domain.model.TfjOperationalSnapshot;
import com.sixpay.accounting.domain.model.TfjStatus;
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

@WebMvcTest(TfjOperationalQueryController.class)
@ContextConfiguration(classes = {
        TfjOperationalQueryController.class,
        TfjOperationalQueryExceptionHandler.class,
        TfjOperationalQueryControllerTest.MethodSecurityConfiguration.class
})
class TfjOperationalQueryControllerTest {

    private static final UUID CONFIRMATION_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TfjOperationalQueryUseCase query;

    @Test
    @WithMockUser(
            username = "auditor@sixpay",
            authorities = {"ROLE_AUDITOR", "SCOPE_accounting.read"}
    )
    void searchesOperationalConfirmations() throws Exception {
        when(query.search(
                LocalDate.of(2026, 9, 11),
                TfjOperationalCategory.QUARANTINED_UNMATCHED,
                "PAY-001",
                "BANK-POST-001",
                0,
                20
        )).thenReturn(
                new TfjOperationalQueryUseCase.Page(
                        List.of(snapshot()),
                        0,
                        20,
                        1
                )
        );

        mockMvc.perform(
                        get("/internal/api/v1/accounting-tfj-operations")
                                .queryParam("businessDate", "2026-09-11")
                                .queryParam(
                                        "category",
                                        "QUARANTINED_UNMATCHED"
                                )
                                .queryParam(
                                        "paymentReference",
                                        "PAY-001"
                                )
                                .queryParam(
                                        "bankPostingReference",
                                        "BANK-POST-001"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].confirmationId")
                        .value(CONFIRMATION_ID.toString()))
                .andExpect(jsonPath("$.content[0].category")
                        .value("QUARANTINED_UNMATCHED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(query).search(
                LocalDate.of(2026, 9, 11),
                TfjOperationalCategory.QUARANTINED_UNMATCHED,
                "PAY-001",
                "BANK-POST-001",
                0,
                20
        );
    }

    @Test
    @WithMockUser(
            username = "manager@sixpay",
            authorities = {"ROLE_MANAGER", "SCOPE_accounting.read"}
    )
    void readsOperationalConfirmationDetail() throws Exception {
        when(query.findByConfirmationId(CONFIRMATION_ID))
                .thenReturn(snapshot());

        mockMvc.perform(
                        get(
                                "/internal/api/v1/accounting-tfj-operations/{confirmationId}",
                                CONFIRMATION_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference")
                        .value("PAY-001"))
                .andExpect(jsonPath("$.matchStatus")
                        .value("UNMATCHED"));
    }

    @Test
    @WithMockUser(
            username = "admin@sixpay",
            authorities = {"ROLE_ADMIN", "SCOPE_accounting.read"}
    )
    void allowsAdminWithAccountingReadScope() throws Exception {
        when(query.search(
                null,
                null,
                null,
                null,
                0,
                20
        )).thenReturn(
                new TfjOperationalQueryUseCase.Page(
                        List.of(),
                        0,
                        20,
                        0
                )
        );

        mockMvc.perform(
                        get("/internal/api/v1/accounting-tfj-operations")
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(
            username = "auditor-without-scope@sixpay",
            authorities = "ROLE_AUDITOR"
    )
    void rejectsApprovedRoleWithoutAccountingReadScope() throws Exception {
        mockMvc.perform(
                        get("/internal/api/v1/accounting-tfj-operations")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "partner@sixpay",
            authorities = {"ROLE_PARTNER", "SCOPE_accounting.read"}
    )
    void rejectsReadScopeWithoutApprovedOperatorRole() throws Exception {
        mockMvc.perform(
                        get("/internal/api/v1/accounting-tfj-operations")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnauthenticatedQuery() throws Exception {
        mockMvc.perform(
                        get("/internal/api/v1/accounting-tfj-operations")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(
            username = "admin@sixpay",
            authorities = {"ROLE_ADMIN", "SCOPE_accounting.read"}
    )
    void returns404ForUnknownConfirmation() throws Exception {
        when(query.findByConfirmationId(any()))
                .thenThrow(
                        new TfjOperationalConfirmationNotFoundException(
                                CONFIRMATION_ID
                        )
                );

        mockMvc.perform(
                        get(
                                "/internal/api/v1/accounting-tfj-operations/{confirmationId}",
                                CONFIRMATION_ID
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title")
                        .value("TFJ operational confirmation not found"));
    }

    private static TfjOperationalSnapshot snapshot() {
        return new TfjOperationalSnapshot(
                CONFIRMATION_ID,
                "LAREGIONALE",
                LocalDate.of(2026, 9, 11),
                "PAY-001",
                "BANK-POST-001",
                "TFJ-BATCH-001",
                TfjStatus.PENDING,
                Instant.parse("2026-09-11T23:00:00Z"),
                TfjObservationChannel.ASYNC_CALLBACK,
                "corr-001",
                TfjMatchStatus.UNMATCHED,
                null,
                null,
                null,
                null,
                TfjOperationalCategory.QUARANTINED_UNMATCHED
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
