package com.sixpay.payment.infrastructure.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCorrelationLoggingFilterTest {

    @Test
    void propagatesValidCorrelationIdAndCleansMdc()
            throws Exception {
        PaymentCorrelationLoggingFilter filter =
                new PaymentCorrelationLoggingFilter();

        UUID correlationId = UUID.randomUUID();

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/internal/api/v1/payments"
                );
        request.addHeader(
                PaymentCorrelationLoggingFilter
                        .CORRELATION_HEADER,
                correlationId.toString()
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertThat(
                response.getHeader(
                        PaymentCorrelationLoggingFilter
                                .CORRELATION_HEADER
                )
        ).isEqualTo(correlationId.toString());

        assertThat(
                MDC.get(
                        PaymentCorrelationLoggingFilter
                                .MDC_CORRELATION_KEY
                )
        ).isNull();
    }

    @Test
    void replacesInvalidCorrelationId() throws Exception {
        PaymentCorrelationLoggingFilter filter =
                new PaymentCorrelationLoggingFilter();

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/internal/api/v1/payments"
                );
        request.addHeader(
                PaymentCorrelationLoggingFilter
                        .CORRELATION_HEADER,
                "not-a-uuid"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                new MockFilterChain()
        );

        assertThat(
                UUID.fromString(
                        response.getHeader(
                                PaymentCorrelationLoggingFilter
                                        .CORRELATION_HEADER
                        )
                )
        ).isNotNull();
    }
}
