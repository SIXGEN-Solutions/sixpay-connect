package com.sixpay.payment.api;

import com.sixpay.payment.api.response.PaymentQueryResponses;
import com.sixpay.payment.application.port.in.PaymentProjectionQueryUseCase;
import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.security.PaymentAccessPolicy;
import com.sixpay.payment.application.view.PaymentProjectionViews;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
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

@WebMvcTest(PaymentQueryController.class)
@ContextConfiguration(classes = {
        PaymentQueryController.class,
        PaymentApiExceptionHandler.class,
        PaymentQueryControllerTest.MethodSecurityConfiguration.class
})
class PaymentQueryControllerTest {

    private static final UUID CORRELATION_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "22222222-2222-4222-8222-222222222222"
            );

    private static final Instant SNAPSHOT =
            Instant.parse("2026-08-09T16:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentProjectionQueryUseCase queryUseCase;

    @MockitoBean
    private PaymentApiMapper mapper;

    @MockitoBean(name = "paymentAccessPolicy")
    private PaymentAccessPolicy paymentAccessPolicy;

    @BeforeEach
    void allowPublishedReadOperationsByDefault() {
        when(paymentAccessPolicy.canSearch()).thenReturn(true);
        when(paymentAccessPolicy.canRead()).thenReturn(true);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void searchReturnsMappedPageAndEchoesCorrelationId()
            throws Exception {

        PaymentProjectionViews.SearchPage page =
                mock(PaymentProjectionViews.SearchPage.class);

        PaymentQueryResponses.PaymentSearchPageResponse response =
                new PaymentQueryResponses.PaymentSearchPageResponse(
                        List.of(),
                        0,
                        false,
                        null,
                        SNAPSHOT
                );

        when(queryUseCase.search(
                any(SearchPaymentProjectionsQuery.class)
        )).thenReturn(page);

        when(mapper.toResponse(page))
                .thenReturn(response);

        mockMvc.perform(
                        get("/internal/api/v1/payments")
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
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.items").isArray());

        verify(queryUseCase)
                .search(any(SearchPaymentProjectionsQuery.class));
        verify(mapper).toResponse(page);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void searchRejectsMissingCorrelationHeader()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/payments")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(queryUseCase);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void searchRejectsInvalidCorrelationUuid()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/payments")
                                .header(
                                        "X-Correlation-ID",
                                        "not-a-uuid"
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(queryUseCase);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void searchRejectsPageSizeAboveContractMaximum()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/payments")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                                .queryParam("size", "201")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(queryUseCase);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void searchRejectsInvalidCurrency()
            throws Exception {

        mockMvc.perform(
                        get("/internal/api/v1/payments")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                                .queryParam("currency", "xaf")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_REQUEST")
                );

        verifyNoInteractions(queryUseCase);
    }

    @Test
    @WithMockUser(
            username = "authenticated-without-payment-access@sixpay"
    )
    void searchReturnsForbiddenWhenAccessPolicyRejectsCaller()
            throws Exception {

        when(paymentAccessPolicy.canSearch())
                .thenReturn(false);

        mockMvc.perform(
                        get("/internal/api/v1/payments")
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(queryUseCase);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void getReturnsMappedDetailAndEchoesCorrelationId()
            throws Exception {

        PaymentProjectionViews.Detail detail =
                mock(PaymentProjectionViews.Detail.class);

        PaymentQueryResponses.PaymentDetailResponse response =
                mock(PaymentQueryResponses.PaymentDetailResponse.class);

        when(queryUseCase.findById(PAYMENT_ID))
                .thenReturn(Optional.of(detail));

        when(mapper.toResponse(detail))
                .thenReturn(response);

        mockMvc.perform(
                        get(
                                "/internal/api/v1/payments/{paymentId}",
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
                );

        verify(queryUseCase).findById(PAYMENT_ID);
        verify(mapper).toResponse(detail);
    }

    @Test
    @WithMockUser(
            username = "payment-reader@sixpay",
            authorities = "SCOPE_payment.read"
    )
    void getReturnsNotFoundForUnknownPayment()
            throws Exception {

        when(queryUseCase.findById(PAYMENT_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/internal/api/v1/payments/{paymentId}",
                                PAYMENT_ID
                        )
                                .header(
                                        "X-Correlation-ID",
                                        CORRELATION_ID
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("PAYMENT_NOT_FOUND")
                );

        verify(queryUseCase).findById(PAYMENT_ID);
        verifyNoInteractions(mapper);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
