package com.sixpay.partner.api;

import com.sixpay.partner.application.port.in.PartnerListQueryUseCase;
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
    void listsPartnersForInternalRole() throws Exception {
        var now = Instant.parse("2026-08-08T12:00:00Z");
        when(query.list(0, 20)).thenReturn(
                new PartnerPage(
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
                        0,
                        20,
                        1,
                        1
                )
        );

        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("page", "0")
                                .queryParam("size", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id")
                        .value(PARTNER_ID.toString()))
                .andExpect(jsonPath("$.items[0].legalName")
                        .value("TresorPay"))
                .andExpect(jsonPath("$.items[0].status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(query).list(0, 20);
    }

    @Test
    @WithMockUser(
            username = "reader@sixpay",
            authorities = "SCOPE_partner.read"
    )
    void listsPartnersForPartnerReadScope() throws Exception {
        when(query.list(0, 20)).thenReturn(
                new PartnerPage(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/api/v1/partners"))
                .andExpect(status().isOk());

        verify(query).list(0, 20);
    }

    @Test
    @WithMockUser(username = "external", roles = "PARTNER")
    void forbidsExternalPartnerCatalogAccess() throws Exception {
        mockMvc.perform(get("/api/v1/partners"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@sixpay", roles = "ADMIN")
    void rejectsPageSizeAboveContractLimit() throws Exception {
        mockMvc.perform(
                        get("/api/v1/partners")
                                .queryParam("size", "101")
                )
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
