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
            assertThat(authentication.getName()).isEqualTo("sixpay-partner-test");
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
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void rejectsInvalidJwt() throws Exception {
        when(decoder.decode("bad")).thenThrow(new BadJwtException("bad signature"));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("bad"), response, (req, res) -> fail("must not proceed"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void rejectsMissingScope() throws Exception {
        when(decoder.decode("no-scope")).thenReturn(jwt("payment.read", "sixpay-partner-test"));
        var response = new MockHttpServletResponse();
        filter.doFilter(request("no-scope"), response, (req, res) -> fail("must not proceed"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    void acceptsTabSeparatedScopes() throws Exception {
        when(decoder.decode("multiple-scopes"))
                .thenReturn(jwt("payment.read\tpayment.initiate", "sixpay-partner-test"));
        var response = new MockHttpServletResponse();
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        filter.doFilter(request("multiple-scopes"), response,
                (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
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

    @Test
    void authenticatesAllConfirmationOperationsWithTheirOwnScopes() throws Exception {
        String reference = "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV";
        String base = "/v1/payments/" + reference + "/confirmation-challenge";
        String[][] operations = {
                {"GET", base, "payment.confirmation.read"},
                {"POST", base + "/verify", "payment.confirmation.verify"},
                {"POST", base + "/resend", "payment.confirmation.resend"}
        };
        for (String[] operation : operations) {
            var request = new MockHttpServletRequest(operation[0], operation[1]);
            request.addHeader("Authorization", "Bearer ok");
            when(decoder.decode("ok")).thenReturn(jwt(operation[2], "client-a"));
            var response = new MockHttpServletResponse();
            var reached = new java.util.concurrent.atomic.AtomicBoolean();
            filter.doFilter(request, response, (req, res) -> {
                assertThat(SecurityContextHolder.getContext().getAuthentication()
                        .getAuthorities()).extracting("authority")
                        .containsExactly("SCOPE_" + operation[2]);
                reached.set(true);
            });
            assertThat(reached).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
            clearInvocations(decoder);
        }
    }

    @Test
    void confirmationRoutesRejectMissingJwtAndWrongScopeBeforeController() throws Exception {
        String uri = "/v1/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV/confirmation-challenge/verify";
        var missing = new MockHttpServletRequest("POST", uri);
        var missingResponse = new MockHttpServletResponse();
        filter.doFilter(missing, missingResponse, (req,res) -> fail("must reject"));
        assertThat(missingResponse.getStatus()).isEqualTo(401);

        var wrong = new MockHttpServletRequest("POST", uri);
        wrong.addHeader("Authorization", "Bearer wrong");
        when(decoder.decode("wrong")).thenReturn(jwt("payment.initiate", "client-a"));
        var wrongResponse = new MockHttpServletResponse();
        filter.doFilter(wrong, wrongResponse, (req,res) -> fail("must reject"));
        assertThat(wrongResponse.getStatus()).isEqualTo(403);
    }

    @Test
    void confirmationRejectsInvalidJwtAndMissingAzp() throws Exception {
        String uri = "/v1/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV/confirmation-challenge";
        var invalid = new MockHttpServletRequest("GET", uri);
        invalid.addHeader("Authorization", "Bearer invalid-confirmation");
        when(decoder.decode("invalid-confirmation"))
                .thenThrow(new BadJwtException("bad signature"));
        var invalidResponse = new MockHttpServletResponse();
        filter.doFilter(invalid, invalidResponse, (req,res) -> fail("must reject"));
        assertThat(invalidResponse.getStatus()).isEqualTo(401);

        var noAzp = new MockHttpServletRequest("GET", uri);
        noAzp.addHeader("Authorization", "Bearer no-azp-confirmation");
        when(decoder.decode("no-azp-confirmation"))
                .thenReturn(jwt("payment.confirmation.read", ""));
        var noAzpResponse = new MockHttpServletResponse();
        filter.doFilter(noAzp, noAzpResponse, (req,res) -> fail("must reject"));
        assertThat(noAzpResponse.getStatus()).isEqualTo(401);
    }

    @Test
    void unknownPaymentRoutesAreNotIntercepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/payments/sample/other");
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
        verifyNoInteractions(decoder);
    }

    @Test
    void consultationRequiresMachineJwtAndReadScope() throws Exception {
        String uri = "/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV";
        var missing = new MockHttpServletRequest("GET", uri);
        var responseMissing = new MockHttpServletResponse();
        filter.doFilter(missing, responseMissing, (req,res) -> fail("must reject"));
        assertThat(responseMissing.getStatus()).isEqualTo(401);

        when(decoder.decode("wrong")).thenReturn(jwt("payment.initiate", "client-a"));
        var wrong = new MockHttpServletRequest("GET", uri);
        wrong.addHeader("Authorization", "Bearer wrong");
        var responseWrong = new MockHttpServletResponse();
        filter.doFilter(wrong, responseWrong, (req,res) -> fail("must reject"));
        assertThat(responseWrong.getStatus()).isEqualTo(403);

        when(decoder.decode("ok")).thenReturn(jwt("payment.read", "client-a"));
        var ok = new MockHttpServletRequest("GET", uri);
        ok.addHeader("Authorization", "Bearer ok");
        var responseOk = new MockHttpServletResponse();
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        filter.doFilter(ok, responseOk, (req,res) -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities()).extracting("authority")
                    .containsExactly("SCOPE_payment.read");
            reached.set(true);
        });
        assertThat(reached).isTrue();
        assertThat(responseOk.getStatus()).isEqualTo(200);
    }

    @Test
    void consultationOnlyInterceptsExactReferencePathAndGet() throws Exception {
        var wrongMethod = new MockHttpServletRequest("POST",
                "/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV");
        var extraSegment = new MockHttpServletRequest("GET",
                "/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV/other");
        assertThat(M2mJwtAuthenticationFilter.isPartnerM2mRequest(wrongMethod)).isFalse();
        assertThat(M2mJwtAuthenticationFilter.isPartnerM2mRequest(extraSegment)).isFalse();
    }

    private static void fail(String message) { throw new AssertionError(message); }
}
