package com.sixpay.security.infrastructure.authentication.machine;

import com.sixpay.security.authentication.AuthenticatedMachineIdentity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Partner M2M authentication for initiation and confirmation boundaries.
 * Human LOCAL, LDAP, and OIDC authentication does not use this filter.
 * The mTLS gateway requirement remains a separate mandatory contract control.
 */
public final class M2mJwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String INITIATE = "/v1/payments/initiate";
    private static final String CONSULTATION_PREFIX = "/api/v1/integrations/payments/";
    private static final java.util.regex.Pattern PAYMENT_REFERENCE =
            java.util.regex.Pattern.compile("PAY-[0-9A-HJKMNP-TV-Z]{26}");
    private static final String CHALLENGE_SUFFIX = "/confirmation-challenge";
    private static final String VERIFY_SUFFIX = "/verify";
    private static final String RESEND_SUFFIX = "/resend";
    private final JwtDecoder decoder;

    public M2mJwtAuthenticationFilter(JwtDecoder decoder) {
        this.decoder = java.util.Objects.requireNonNull(decoder);
    }

    public static boolean isInitiateRequest(HttpServletRequest request) {
        return "payment.initiate".equals(requiredScope(request));
    }

    /** Only exact approved paths/methods are M2M; no catch-all prefix match. */
    public static boolean isPartnerM2mRequest(HttpServletRequest request) {
        return requiredScope(request) != null;
    }

    static String requiredScope(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty()) {
            if (!path.startsWith(context + "/")) return null;
            path = path.substring(context.length());
        }
        String method = request.getMethod();
        if ("GET".equals(method) && path.startsWith(CONSULTATION_PREFIX)
                && PAYMENT_REFERENCE.matcher(
                        path.substring(CONSULTATION_PREFIX.length())).matches()) {
            return "payment.read";
        }
        if ("POST".equals(method) && INITIATE.equals(path)) {
            return "payment.initiate";
        }
        String prefix = "/v1/payments/";
        if (!path.startsWith(prefix)) return null;
        String remainder = path.substring(prefix.length());
        int slash = remainder.indexOf('/');
        // An opaque public payment reference must occupy exactly one segment.
        if (slash <= 0) return null;
        String suffix = remainder.substring(slash);
        if ("GET".equals(method) && CHALLENGE_SUFFIX.equals(suffix)) {
            return "payment.confirmation.read";
        }
        if ("POST".equals(method) && (CHALLENGE_SUFFIX + VERIFY_SUFFIX).equals(suffix)) {
            return "payment.confirmation.verify";
        }
        if ("POST".equals(method) && (CHALLENGE_SUFFIX + RESEND_SUFFIX).equals(suffix)) {
            return "payment.confirmation.resend";
        }
        return null;
    }

    public static JwtDecoder decoder(String issuer, String jwkSetUri, String audience) {
        if (blank(issuer) || blank(jwkSetUri) || blank(audience)) {
            throw new IllegalStateException(
                    "M2M JWT issuer, JWK set URI and audience are mandatory when enabled");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri.strip()).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience().contains(audience.strip())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Invalid M2M audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer.strip()), audienceValidator));
        return decoder;
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    private static void reject(HttpServletResponse response, int status) {
        response.setStatus(status);
        response.setContentLength(0);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isPartnerM2mRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final Jwt jwt;
        try {
            jwt = decoder.decode(authorization.substring(7).strip());
        } catch (JwtException exception) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String required = requiredScope(request);
        String scope = jwt.getClaimAsString("scope");
        if (required == null || blank(scope) || Arrays.stream(scope.strip().split("\\s+"))
                .noneMatch(required::equals)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 'azp' is the stable Keycloak OAuth2 client identifier. Keycloak's
        // service-account 'sub' is a different, potentially recreated UUID.
        String clientId = jwt.getClaimAsString("azp");
        if (blank(clientId)) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        var machine = new AuthenticatedMachineIdentity(clientId.strip());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                machine, null, List.of(new SimpleGrantedAuthority("SCOPE_" + required)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
