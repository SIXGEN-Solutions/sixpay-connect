package com.sixpay.payment.infrastructure.tresorpay;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;

public final class TresorPayRequestGuard {

    private static final String CERTIFICATE_ATTRIBUTE =
            "jakarta.servlet.request.X509Certificate";

    private final TresorPayIntegrationProperties properties;
    private final TresorPayNonceStore nonceStore;
    private final TresorPayRateLimiter rateLimiter;
    private final StructuredTresorPayAccessAudit audit;
    private final Clock clock;

    public TresorPayRequestGuard(
            TresorPayIntegrationProperties properties,
            TresorPayNonceStore nonceStore,
            TresorPayRateLimiter rateLimiter,
            StructuredTresorPayAccessAudit audit,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties);
        this.nonceStore = Objects.requireNonNull(nonceStore);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
        this.audit = Objects.requireNonNull(audit);
        this.clock = Objects.requireNonNull(clock);
    }

    public void validateTransportRequest(
            HttpServletRequest request
    ) {
        String correlationId = request.getHeader(
                IntegrationHttpHeaders.CORRELATION_ID
        );
        String partnerId = "unknown";

        try {
            Authentication authentication = requireAuthentication();
            partnerId = authentication.getName();

            requireJwt(authentication, partnerId);
            requireMtls(request);
            requireApiKey(request);
            requireFreshRequest(request, partnerId);
            requireRateLimit(partnerId);

            audit.accepted(partnerId, null, correlationId);
        } catch (TresorPayRequestRejectedException rejection) {
            audit.rejected(
                    partnerId,
                    rejection.code(),
                    correlationId
            );
            throw rejection;
        }
    }

    private Authentication requireAuthentication() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw reject(
                    HttpStatus.UNAUTHORIZED,
                    TresorPayErrorCode.AUTHENTICATION_REQUIRED,
                    "Authentication is required"
            );
        }

        return authentication;
    }

    private void requireJwt(
            Authentication authentication,
            String authenticatedPartner
    ) {
        if (!properties.security().oauth2Required()) {
            return;
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw reject(
                    HttpStatus.UNAUTHORIZED,
                    TresorPayErrorCode.INVALID_ACCESS_TOKEN,
                    "A valid OAuth2 access token is required"
            );
        }

        if (!jwt.getAudience().contains(
                properties.security().audience()
        )) {
            throw reject(
                    HttpStatus.UNAUTHORIZED,
                    TresorPayErrorCode.INVALID_ACCESS_TOKEN,
                    "Access token audience is invalid"
            );
        }

        String scopes = Objects.toString(
                jwt.getClaims().get("scope"),
                ""
        );

        boolean requiredScopePresent = Arrays
                .stream(scopes.split("\\s+"))
                .filter(value -> !value.isBlank())
                .anyMatch(
                        properties.security()
                                .requiredScope()::equals
                );

        if (!requiredScopePresent) {
            throw reject(
                    HttpStatus.FORBIDDEN,
                    TresorPayErrorCode.SCOPE_NOT_GRANTED,
                    "Required scope is not granted"
            );
        }

        Object partnerClaim = jwt.getClaims().get(
                properties.security().partnerClaim()
        );

        if (partnerClaim != null
                && !partnerClaim.toString()
                .equals(authenticatedPartner)) {
            throw reject(
                    HttpStatus.FORBIDDEN,
                    TresorPayErrorCode.PARTNER_IDENTITY_MISMATCH,
                    "Token partner identity is inconsistent"
            );
        }
    }

    private void requireMtls(
            HttpServletRequest request
    ) {
        if (!properties.security().mtlsRequired()) {
            return;
        }

        Object value = request.getAttribute(
                CERTIFICATE_ATTRIBUTE
        );

        if (!(value instanceof X509Certificate[] certificates)
                || certificates.length == 0) {
            throw reject(
                    HttpStatus.UNAUTHORIZED,
                    TresorPayErrorCode.INVALID_CLIENT_CERTIFICATE,
                    "A valid client certificate is required"
            );
        }
    }

    private void requireApiKey(
            HttpServletRequest request
    ) {
        if (!properties.security().apiKeyEnabled()) {
            return;
        }

        String supplied = request.getHeader(
                properties.security().apiKeyHeader()
        );

        if (supplied == null
                || !MessageDigest.isEqual(
                        supplied.getBytes(StandardCharsets.UTF_8),
                        properties.security()
                                .apiKeyValue()
                                .getBytes(StandardCharsets.UTF_8)
                )) {
            throw reject(
                    HttpStatus.UNAUTHORIZED,
                    TresorPayErrorCode.API_KEY_INVALID,
                    "Partner credential is invalid"
            );
        }
    }

    private void requireFreshRequest(
            HttpServletRequest request,
            String partnerId
    ) {
        if (!properties.antiReplay().enabled()) {
            return;
        }

        String timestampValue = request.getHeader(
                TresorPayHeaders.REQUEST_TIMESTAMP
        );
        String nonce = request.getHeader(
                TresorPayHeaders.REQUEST_NONCE
        );

        if (timestampValue == null
                || nonce == null
                || nonce.isBlank()) {
            throw reject(
                    HttpStatus.BAD_REQUEST,
                    TresorPayErrorCode.MISSING_HEADER,
                    "Anti-replay headers are required"
            );
        }

        Instant timestamp;
        try {
            timestamp = Instant.parse(timestampValue);
        } catch (DateTimeParseException exception) {
            throw reject(
                    HttpStatus.BAD_REQUEST,
                    TresorPayErrorCode.REQUEST_TIMESTAMP_INVALID,
                    "Request timestamp is invalid"
            );
        }

        Instant now = clock.instant();
        Duration difference =
                Duration.between(timestamp, now).abs();

        if (difference.compareTo(
                properties.antiReplay().allowedClockSkew()
        ) > 0) {
            throw reject(
                    HttpStatus.UNAUTHORIZED,
                    TresorPayErrorCode.REQUEST_TIMESTAMP_INVALID,
                    "Request timestamp is outside the accepted window"
            );
        }

        if (!nonceStore.registerIfAbsent(
                partnerId,
                nonce.strip(),
                now.plus(properties.antiReplay().nonceTtl())
        )) {
            throw reject(
                    HttpStatus.CONFLICT,
                    TresorPayErrorCode.REPLAY_DETECTED,
                    "Request replay detected"
            );
        }
    }

    private void requireRateLimit(
            String partnerId
    ) {
        if (!properties.rateLimit().enabled()) {
            return;
        }

        TresorPayRateLimiter.RateLimitDecision decision =
                rateLimiter.acquire(partnerId);

        if (!decision.allowed()) {
            throw new TresorPayRequestRejectedException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    TresorPayErrorCode.RATE_LIMIT_EXCEEDED,
                    "Partner rate limit exceeded",
                    decision.retryAfterSeconds()
            );
        }
    }

    private static TresorPayRequestRejectedException reject(
            HttpStatus status,
            TresorPayErrorCode code,
            String message
    ) {
        return new TresorPayRequestRejectedException(
                status,
                code,
                message
        );
    }
}
