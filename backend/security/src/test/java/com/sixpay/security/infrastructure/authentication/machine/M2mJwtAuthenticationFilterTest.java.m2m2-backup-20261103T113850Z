package com.sixpay.security.infrastructure.authentication.machine;

import com.sixpay.security.authentication.AuthenticatedMachineIdentity;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class M2mJwtAuthenticationFilterTest {
    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final M2mJwtAuthenticationFilter filter = new M2mJwtAuthenticationFilter(decoder);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private MockHttpServletRequest request(String token) {
        var request = new MockHttpServletRequest("POST", "/v1/payments/initiate");
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private Jwt jwt(String scope, String clientId) {
        return Jwt.withTokenValue("signed-token")
                .header("alg", "RS256")
                .subject("service-account-uuid")
                .issuedAt(Instant.parse("2026-11-02T12:00:00Z"))
                .expiresAt(Instant.parse("2026-11-02T12:05:00Z"))
                .claim("scope", scope)
                .claim("azp", clientId)
                .audience(List.of("sixpay-api"))
                .build();
    }

    @Test
    void establishesMachinePrincipalWithInitiationAuthority() throws Exception {
        when(decoder.decode("ok")).thenReturn(jwt("payment.initiate", "sixpay-partner-test"));
        var response = new MockHttpServletResponse();
        var observed = new AtomicReference<AuthenticatedMachineIdentity>();
        FilterChain chain = (request, ignored) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getAuthorities())
                    .extracting("authority").containsExactly("SCOPE_payment.initiate");
            observed.set((AuthenticatedMachineIdentity) authentication.getPrincipal());
        };
        filter.doFilter(request("ok"), response, chain);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(observed.get().subject()).isEqualTo("sixpay-partner-test");
    }

    @Test
    void rejectsMissingToken() throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request(null), response, (req, res) -> fail("must not proceed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsInvalidJwt() throws Exception {
        when(decoder.decode("bad")).thenThrow(new BadJwtException("bad signature"));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("bad"), response, (req, res) -> fail("must not proceed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsMissingScope() throws Exception {
        when(decoder.decode("no-scope")).thenReturn(jwt("payment.read", "sixpay-partner-test"));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("no-scope"), response, (req, res) -> fail("must not proceed"));
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void rejectsTokenWithoutClientIdentity() throws Exception {
        when(decoder.decode("no-client")).thenReturn(jwt("payment.initiate", ""));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("no-client"), response, (req, res) -> fail("must not proceed"));
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void doesNotInterceptOtherEndpoints() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        var response = new MockHttpServletResponse();
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
        verifyNoInteractions(decoder);
    }

    private static void fail(String message) { throw new AssertionError(message); }
}
