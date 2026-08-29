package com.sixpay.partner.api;

import com.sixpay.partner.application.port.input.PartnerListQueryUseCase;
import com.sixpay.partner.application.port.output.PartnerOperationMetrics;
import com.sixpay.partner.application.view.PartnerPage;
import com.sixpay.partner.application.view.PartnerSummaryView;
import com.sixpay.partner.domain.model.PartnerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartnerCatalogController.class)
@ContextConfiguration(classes = {
        PartnerCatalogController.class,
        PartnerApiExceptionHandler.class,
        PartnerCatalogControllerTest.MethodSecurityConfiguration.class
})
class PartnerCatalogControllerTest {

    private static final UUID PARTNER_ID =
            UUID.fromString("f88166d1-39df-4900-bb31-1700d25c3bfa");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartnerListQueryUseCase query;

    @MockitoBean
    private PartnerOperationMetrics metrics;

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void listsTheDefaultFirstPage() throws Exception {
        when(query.list(0, 20)).thenReturn(page(0, 20));

        mockMvc.perform(get("/api/v1/partners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(41))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(query).list(0, 20);
    }

    @Test
    @WithMockUser(username = "manager@sixpay", roles = "MANAGER")
    void forwardsPageOneWithoutFallingBackToPageZero() throws Exception {
        when(query.list(1, 20)).thenReturn(page(1, 20));

        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("page", "1")
                                .queryParam("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));

        verify(query).list(1, 20);
    }

    @Test
    @WithMockUser(
            username = "reader@sixpay",
            authorities = "SCOPE_partner.read"
    )
    void allowsThePublishedPartnerReadScope() throws Exception {
        when(query.list(0, 20)).thenReturn(
                new PartnerPage(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/api/v1/partners"))
                .andExpect(status().isOk());

        verify(query).list(0, 20);
    }

    @Test
    @WithMockUser(username = "auditor@sixpay", roles = "AUDITOR")
    void preservesInternalAuditorReadCompatibility() throws Exception {
        when(query.list(0, 20)).thenReturn(
                new PartnerPage(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/api/v1/partners"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "external", roles = "PARTNER")
    void forbidsPartnerRoleWithoutPartnerReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/partners"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(query);
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("page", "-1")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(query);
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsZeroPageSize() throws Exception {
        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("size", "0")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(query);
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void acceptsMaximumPageSize() throws Exception {
        when(query.list(0, 100)).thenReturn(
                new PartnerPage(List.of(), 0, 100, 0, 0)
        );

        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("size", "100")
                )
                .andExpect(status().isOk());

        verify(query).list(0, 100);
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsPageSizeAboveContractLimit() throws Exception {
        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("size", "101")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(query);
    }

    private static PartnerPage page(int page, int size) {
        var now = Instant.parse("2026-08-08T12:00:00Z");

        return new PartnerPage(
                List.of(
                        new PartnerSummaryView(
                                PARTNER_ID,
                                "TresorPay",
                                "Operations TresorPay",
                                "operations@tresorpay.cm",
                                Set.of("PAYMENT"),
                                PartnerStatus.ACTIVE,
                                now,
                                now
                        )
                ),
                page,
                size,
                41,
                3
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
