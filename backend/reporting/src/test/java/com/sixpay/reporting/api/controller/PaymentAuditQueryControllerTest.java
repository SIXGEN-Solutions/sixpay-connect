package com.sixpay.reporting.api.controller;

import com.sixpay.reporting.api.dto.PaymentTimelinePageResponse;
import com.sixpay.reporting.api.exception.PaymentAuditQueryExceptionHandler;
import com.sixpay.reporting.api.mapper.PaymentAuditQueryApiMapper;
import com.sixpay.reporting.application.port.input.GetPaymentAuditRecordUseCase;
import com.sixpay.reporting.application.port.input.GetPaymentTimelineUseCase;
import com.sixpay.reporting.application.port.input.SearchPaymentAuditRecordsUseCase;
import com.sixpay.reporting.application.port.output.PaymentAuditAccessRecorder;
import com.sixpay.reporting.application.query.PaymentTimelinePage;
import com.sixpay.reporting.application.query.PaymentTimelineQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentAuditQueryController.class)
@ContextConfiguration(classes = {
        PaymentAuditQueryController.class,
        PaymentAuditQueryExceptionHandler.class,
        PaymentAuditQueryControllerTest.MethodSecurityConfiguration.class,
        PaymentAuditQueryControllerTest.ClockConfiguration.class
})
class PaymentAuditQueryControllerTest {

    private static final UUID CORRELATION_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    private static final UUID PAYMENT_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-09T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPaymentTimelineUseCase timelineUseCase;

    @MockitoBean
    private SearchPaymentAuditRecordsUseCase searchUseCase;

    @MockitoBean
    private GetPaymentAuditRecordUseCase getUseCase;

    @MockitoBean
    private PaymentAuditQueryApiMapper mapper;

    @MockitoBean
    private PaymentAuditAccessRecorder accessRecorder;

    @Test
    @WithMockUser(
            username = "auditor@sixpay",
            authorities = "SCOPE_payment.audit.read"
    )
    void timelineReturnsPageAndEchoesCorrelationId() throws Exception {
        PaymentTimelinePage page = mock(PaymentTimelinePage.class);

        PaymentTimelinePageResponse response =
                new PaymentTimelinePageResponse(
                        List.of(),
                        0,
                        false,
                        null,
                        SNAPSHOT
                );

        when(timelineUseCase.getTimeline(
                any(PaymentTimelineQuery.class)
        )).thenReturn(page);
        when(mapper.toResponse(page)).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/internal/api/v1/payments/{paymentId}/timeline",
                                PAYMENT_ID
                        )
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "X-Correlation-ID",
                                CORRELATION_ID.toString()
                        )
                )
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(timelineUseCase)
                .getTimeline(any(PaymentTimelineQuery.class));
        verify(accessRecorder)
                .recordSuccessfulRead(
                        "GET_PAYMENT_TIMELINE",
                        "PAYMENT",
                        PAYMENT_ID.toString(),
                        CORRELATION_ID,
                        "auditor@sixpay"
                );
    }

    @Test
    @WithMockUser(username = "user-without-audit-scope@sixpay")
    void timelineForbidsCallerWithoutReadScope() throws Exception {
        mockMvc.perform(
                        get(
                                "/internal/api/v1/payments/{paymentId}/timeline",
                                PAYMENT_ID
                        )
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(timelineUseCase);
    }

    @Test
    @WithMockUser(
            username = "auditor@sixpay",
            authorities = "SCOPE_payment.audit.read"
    )
    void timelineRejectsPageSizeAboveContractMaximum() throws Exception {
        mockMvc.perform(
                        get(
                                "/internal/api/v1/payments/{paymentId}/timeline",
                                PAYMENT_ID
                        )
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                                .queryParam("size", "201")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("PAYMENT_AUDIT_QUERY_INVALID")
                );

        verifyNoInteractions(timelineUseCase);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Qualifier("reportingAuditClock")
        Clock reportingAuditClock() {
            return Clock.fixed(SNAPSHOT, ZoneOffset.UTC);
        }
    }
}
