package com.sixpay.security.configuration;

import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

public final class AuditingAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final SecurityAuditPort auditPort;

    private final AuthenticationEntryPoint bearerEntryPoint =
            new BearerTokenAuthenticationEntryPoint();

    public AuditingAuthenticationEntryPoint(
            SecurityAuditPort auditPort
    ) {
        this.auditPort =
                Objects.requireNonNull(
                        auditPort
                );
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        String authorization =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (isBearerRequest(authorization)) {

            auditPort.record(
                    new SecurityAuditEvent(
                            SecurityAuditEventType.OIDC_LOGIN_FAILURE,
                            null,
                            null,
                            null,
                            null,
                            "bearer-authentication-failed",
                            Instant.now()
                    )
            );

            /*
             * Preserve the standard OAuth2 Resource Server
             * response contract:
             *
             * HTTP 401
             * +
             * WWW-Authenticate: Bearer ...
             */
            bearerEntryPoint.commence(
                    request,
                    response,
                    authException
            );

            return;
        }

        response.sendError(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized"
        );
    }

    private static boolean isBearerRequest(
            String authorization
    ) {
        return authorization != null
                && authorization.startsWith(
                "Bearer "
        );
    }
}