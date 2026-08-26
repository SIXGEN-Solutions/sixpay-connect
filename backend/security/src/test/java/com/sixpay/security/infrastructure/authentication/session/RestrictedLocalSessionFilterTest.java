package com.sixpay.security.infrastructure.authentication.session;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RestrictedLocalSessionFilterTest {

    @Test
    void blocksBusinessEndpointForRestrictedLocalSession()
            throws Exception {

        SpringSecuritySessionManager sessionManager =
                mock(
                        SpringSecuritySessionManager.class
                );

        when(
                sessionManager
                        .passwordChangeRequired(
                                any()
                        )
        )
                .thenReturn(true);

        RestrictedLocalSessionFilter filter =
                new RestrictedLocalSessionFilter(
                        sessionManager
                );

        MockHttpServletRequest request =
                request(
                        "GET",
                        "/internal/api/v1/payments"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain =
                mock(FilterChain.class);

        assertThatThrownBy(() ->
                filter.doFilter(
                        request,
                        response,
                        chain
                )
        )
                .isInstanceOf(
                        AccessDeniedException.class
                );

        verifyNoInteractions(
                chain
        );
    }

    @Test
    void allowsMeLogoutAndPasswordChangeForRestrictedLocalSession()
            throws Exception {

        SpringSecuritySessionManager sessionManager =
                mock(
                        SpringSecuritySessionManager.class
                );

        when(
                sessionManager
                        .passwordChangeRequired(
                                any()
                        )
        )
                .thenReturn(true);

        RestrictedLocalSessionFilter filter =
                new RestrictedLocalSessionFilter(
                        sessionManager
                );

        assertAllowed(
                filter,
                "GET",
                "/api/v1/auth/me"
        );

        assertAllowed(
                filter,
                "POST",
                "/api/v1/auth/logout"
        );

        assertAllowed(
                filter,
                "POST",
                "/api/v1/auth/password/change"
        );
    }

    @Test
    void allowsNormalSessionWithoutPathRestriction()
            throws Exception {

        SpringSecuritySessionManager sessionManager =
                mock(
                        SpringSecuritySessionManager.class
                );

        when(
                sessionManager
                        .passwordChangeRequired(
                                any()
                        )
        )
                .thenReturn(false);

        RestrictedLocalSessionFilter filter =
                new RestrictedLocalSessionFilter(
                        sessionManager
                );

        assertAllowed(
                filter,
                "GET",
                "/internal/api/v1/payments"
        );
    }

    private static void assertAllowed(
            RestrictedLocalSessionFilter filter,
            String method,
            String path
    ) throws Exception {

        MockHttpServletRequest request =
                request(
                        method,
                        path
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain chain =
                mock(FilterChain.class);

        filter.doFilter(
                request,
                response,
                chain
        );

        verify(chain)
                .doFilter(
                        request,
                        response
                );
    }

    private static MockHttpServletRequest request(
            String method,
            String path
    ) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        method,
                        path
                );

        request.setRequestURI(
                path
        );

        return request;
    }
}
