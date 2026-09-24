package com.sixpay.security.infrastructure.authentication.subscription;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PartnerSubscriptionKeyFilterTest {

    @Test
    void rejectsMissingSubscriptionKeyOnPartnerPaymentBoundary()
            throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        FilterChain chain = mock(FilterChain.class);
        var filter = new PartnerSubscriptionKeyFilter(
                "expected-key"::equals,
                entryPoint
        );
        var request = new MockHttpServletRequest(
                "GET",
                "/api/v1/partners/payments/PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X"
        );
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                org.mockito.ArgumentMatchers.any()
        );
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void acceptsValidSubscriptionKeyOnPartnerPaymentBoundary()
            throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        FilterChain chain = mock(FilterChain.class);
        var filter = new PartnerSubscriptionKeyFilter(
                "expected-key"::equals,
                entryPoint
        );
        var request = new MockHttpServletRequest(
                "GET",
                "/api/v1/partners/payments/PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X"
        );
        request.addHeader(
                PartnerSubscriptionKeyFilter.HEADER_NAME,
                "expected-key"
        );
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(entryPoint, never()).commence(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void ignoresNonPartnerEndpoints() throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        FilterChain chain = mock(FilterChain.class);
        var filter = new PartnerSubscriptionKeyFilter(
                "expected-key"::equals,
                entryPoint
        );
        var request = new MockHttpServletRequest("GET", "/secured");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
