package com.sixpay.security.configuration;

import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

public final class AuditingAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final SecurityAuditPort auditPort;

    public AuditingAuthenticationEntryPoint(SecurityAuditPort auditPort) {
        this.auditPort = Objects.requireNonNull(auditPort);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            auditPort.record(new SecurityAuditEvent(
                    SecurityAuditEventType.OIDC_LOGIN_FAILURE,
                    null,
                    null,
                    null,
                    null,
                    "bearer-authentication-failed",
                    Instant.now()
            ));
        }
        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
    }
}
