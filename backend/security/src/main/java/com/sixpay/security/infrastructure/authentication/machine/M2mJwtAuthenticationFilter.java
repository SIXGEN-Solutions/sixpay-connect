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
 * Partner M2M authentication for the approved payment-initiation boundary only.
 * Human LOCAL, LDAP, and OIDC authentication does not use this filter.
 * The mTLS gateway requirement remains a separate mandatory contract control.
 */
public final class M2mJwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORITY = "SCOPE_payment.initiate";
    private static final String SCOPE = "payment.initiate";
    private final JwtDecoder decoder;

    public M2mJwtAuthenticationFilter(JwtDecoder decoder) {
        this.decoder = java.util.Objects.requireNonNull(decoder);
    }

    public static boolean isInitiateRequest(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && (request.getContextPath() + "/v1/payments/initiate")
                        .equals(request.getRequestURI());
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
        return !isInitiateRequest(request);
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

        String scope = jwt.getClaimAsString("scope");
        if (blank(scope) || Arrays.stream(scope.strip().split("\\s+"))
                .noneMatch(SCOPE::equals)) {
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
                machine, null, List.of(new SimpleGrantedAuthority(AUTHORITY)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
