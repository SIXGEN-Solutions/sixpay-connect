package com.sixpay.reporting.api.controller;

import com.sixpay.reporting.api.dto.PaymentAuditExportJobResponse;
import com.sixpay.reporting.api.exception.PaymentAuditQueryExceptionHandler;
import com.sixpay.reporting.api.mapper.PaymentAuditExportApiMapper;
import com.sixpay.reporting.application.port.input.GetPaymentAuditExportUseCase;
import com.sixpay.reporting.application.port.input.RequestPaymentAuditExportUseCase;
import com.sixpay.reporting.application.port.output.PaymentAuditAccessRecorder;
import com.sixpay.reporting.application.query.PaymentAuditExportJobView;
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
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentAuditExportController.class)
@ContextConfiguration(classes = {
        PaymentAuditExportController.class,
        PaymentAuditQueryExceptionHandler.class,
        PaymentAuditExportControllerTest.MethodSecurityConfiguration.class
})
class PaymentAuditExportControllerTest {

    private static final UUID CORRELATION_ID =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    private static final UUID EXPORT_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestPaymentAuditExportUseCase requestUseCase;

    @MockitoBean
    private GetPaymentAuditExportUseCase getUseCase;

    @MockitoBean
    private PaymentAuditExportApiMapper mapper;

    @MockitoBean
    private PaymentAuditAccessRecorder accessRecorder;

    @Test
    @WithMockUser(
            username = "audit-exporter@sixpay",
            authorities = {
                    "SCOPE_payment.audit.read",
                    "SCOPE_payment.audit.export"
            }
    )
    void getReturnsExportStatusAndEchoesCorrelationId() throws Exception {
        PaymentAuditExportJobView job =
                mock(PaymentAuditExportJobView.class);

        PaymentAuditExportJobResponse response =
                new PaymentAuditExportJobResponse(
                        EXPORT_ID,
                        "PENDING",
                        Instant.parse("2026-08-09T18:00:00Z"),
                        "audit-exporter@sixpay",
                        "pilot validation",
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(getUseCase.get(EXPORT_ID)).thenReturn(job);
        when(mapper.toResponse(job)).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/internal/api/v1/payment-audit-exports/{exportId}",
                                EXPORT_ID
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
                );

        verify(getUseCase).get(EXPORT_ID);
        verify(accessRecorder)
                .recordSuccessfulRead(
                        "GET_PAYMENT_AUDIT_EXPORT",
                        "AUDIT_EXPORT",
                        EXPORT_ID.toString(),
                        CORRELATION_ID,
                        "audit-exporter@sixpay"
                );
    }

    @Test
    @WithMockUser(
            username = "audit-reader-only@sixpay",
            authorities = "SCOPE_payment.audit.read"
    )
    void getRequiresExportScopeInAdditionToReadScope() throws Exception {
        mockMvc.perform(
                        get(
                                "/internal/api/v1/payment-audit-exports/{exportId}",
                                EXPORT_ID
                        )
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(getUseCase);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
