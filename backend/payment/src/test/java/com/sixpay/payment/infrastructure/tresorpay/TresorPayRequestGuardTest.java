package com.sixpay.payment.infrastructure.tresorpay;

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

class TresorPayRequestGuardTest {

    private static final Instant NOW =
            Instant.parse("2026-08-06T14:00:00Z");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsStandaloneTransportRequest() {
        authenticate("TRESOR_PAY");
        guard(10).validateTransportRequest(request("nonce-1"));
    }

    @Test
    void rejectsReplayOfSamePartnerNonce() {
        authenticate("TRESOR_PAY");
        TresorPayRequestGuard guard = guard(10);

        guard.validateTransportRequest(request("nonce-1"));

        assertThatThrownBy(() ->
                guard.validateTransportRequest(request("nonce-1"))
        )
                .isInstanceOf(TresorPayRequestRejectedException.class)
                .extracting("code")
                .isEqualTo(TresorPayErrorCode.REPLAY_DETECTED);
    }

    @Test
    void enforcesPartnerRateLimit() {
        authenticate("TRESOR_PAY");
        TresorPayRequestGuard guard = guard(1);

        guard.validateTransportRequest(request("nonce-1"));

        assertThatThrownBy(() ->
                guard.validateTransportRequest(request("nonce-2"))
        )
                .isInstanceOf(TresorPayRequestRejectedException.class)
                .extracting("code")
                .isEqualTo(TresorPayErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsUnauthenticatedRequest() {
        assertThatThrownBy(() ->
                guard(10).validateTransportRequest(request("nonce-1"))
        )
                .isInstanceOf(TresorPayRequestRejectedException.class)
                .extracting("code")
                .isEqualTo(TresorPayErrorCode.AUTHENTICATION_REQUIRED);
    }

    private static TresorPayRequestGuard guard(int limit) {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        TresorPayIntegrationProperties properties =
                new TresorPayIntegrationProperties(
                        new TresorPayIntegrationProperties.Security(
                                false,
                                false,
                                false,
                                "X-API-Key",
                                null,
                                "sixpay-payment-api",
                                "client_id",
                                "payment.initiate"
                        ),
                        new TresorPayIntegrationProperties.AntiReplay(
                                true,
                                Duration.ofMinutes(5),
                                Duration.ofMinutes(10)
                        ),
                        new TresorPayIntegrationProperties.RateLimit(
                                true,
                                limit
                        ),
                        new TresorPayIntegrationProperties.Callback(
                                true,
                                "RS256",
                                Duration.ofHours(24)
                        ),
                        List.of("tresorpay.cm")
                );

        return new TresorPayRequestGuard(
                properties,
                new InMemoryTresorPayNonceStore(clock),
                new FixedWindowTresorPayRateLimiter(clock, limit),
                new StructuredTresorPayAccessAudit(),
                clock
        );
    }

    private static MockHttpServletRequest request(String nonce) {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                TresorPayHeaders.REQUEST_TIMESTAMP,
                NOW.toString()
        );
        request.addHeader(
                TresorPayHeaders.REQUEST_NONCE,
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
