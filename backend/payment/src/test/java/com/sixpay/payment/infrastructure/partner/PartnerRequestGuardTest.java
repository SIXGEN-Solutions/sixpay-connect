package com.sixpay.payment.infrastructure.partner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerRequestGuardTest {

    private static final Instant NOW =
            Instant.parse("2026-08-06T14:00:00Z");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsStandaloneTransportRequest() {
        authenticate("PARTNER");
        guard(10).validateTransportRequest(request("nonce-1"));
    }

    @Test
    void rejectsReplayOfSamePartnerNonce() {
        authenticate("PARTNER");
        PartnerRequestGuard guard = guard(10);

        guard.validateTransportRequest(request("nonce-1"));

        assertThatThrownBy(() ->
                guard.validateTransportRequest(request("nonce-1"))
        )
                .isInstanceOf(PartnerRequestRejectedException.class)
                .extracting("code")
                .isEqualTo(PartnerRequestErrorCode.REPLAY_DETECTED);
    }

    @Test
    void enforcesPartnerRateLimit() {
        authenticate("PARTNER");
        PartnerRequestGuard guard = guard(1);

        guard.validateTransportRequest(request("nonce-1"));

        assertThatThrownBy(() ->
                guard.validateTransportRequest(request("nonce-2"))
        )
                .isInstanceOf(PartnerRequestRejectedException.class)
                .extracting("code")
                .isEqualTo(PartnerRequestErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsUnauthenticatedRequest() {
        assertThatThrownBy(() ->
                guard(10).validateTransportRequest(request("nonce-1"))
        )
                .isInstanceOf(PartnerRequestRejectedException.class)
                .extracting("code")
                .isEqualTo(PartnerRequestErrorCode.AUTHENTICATION_REQUIRED);
    }

    private static PartnerRequestGuard guard(int limit) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        PartnerIntegrationProperties properties =
                new PartnerIntegrationProperties(
                        new PartnerIntegrationProperties.Security(
                                false,
                                false,
                                false,
                                "X-API-Key",
                                null,
                                "sixpay-payment-api",
                                "client_id",
                                "payment.initiate"
                        ),
                        new PartnerIntegrationProperties.AntiReplay(
                                true,
                                Duration.ofMinutes(5),
                                Duration.ofMinutes(10)
                        ),
                        new PartnerIntegrationProperties.RateLimit(
                                true,
                                limit
                        ),
                        new PartnerIntegrationProperties.Callback(
                                true,
                                "RS256",
                                Duration.ofHours(24)
                        ),
                        List.of("partner.example")
                );

        return new PartnerRequestGuard(
                properties,
                new InMemoryPartnerNonceStore(clock),
                new FixedWindowPartnerRateLimiter(clock, limit),
                new StructuredPartnerAccessAudit(),
                clock
        );
    }

    private static MockHttpServletRequest request(String nonce) {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                PartnerHeaders.REQUEST_TIMESTAMP,
                NOW.toString()
        );
        request.addHeader(
                PartnerHeaders.REQUEST_NONCE,
                nonce
        );

        return request;
    }

    private static void authenticate(String partnerId) {
        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                partnerId,
                                "N/A",
                                List.of()
                        )
                );
    }
}
