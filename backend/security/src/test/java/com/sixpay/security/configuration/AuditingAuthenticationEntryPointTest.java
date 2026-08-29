package com.sixpay.security.configuration;

import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AuditingAuthenticationEntryPointTest {

    @Test
    void bearerFailureIsAuditedAndPreservesOauthBearerChallenge()
            throws Exception {

        SecurityAuditPort auditPort =
                mock(
                        SecurityAuditPort.class
                );

        AuditingAuthenticationEntryPoint entryPoint =
                new AuditingAuthenticationEntryPoint(
                        auditPort
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException(
                        "invalid bearer token"
                )
        );

        assertThat(
                response.getStatus()
        )
                .isEqualTo(401);

        assertThat(
                response.getHeader(
                        "WWW-Authenticate"
                )
        )
                .startsWith("Bearer");

        ArgumentCaptor<SecurityAuditEvent> audit =
                ArgumentCaptor.forClass(
                        SecurityAuditEvent.class
                );

        verify(auditPort)
                .record(
                        audit.capture()
                );

        assertThat(
                audit.getValue()
                        .eventType()
        )
                .isEqualTo(
                        SecurityAuditEventType.OIDC_LOGIN_FAILURE
                );

        assertThat(
                audit.getValue()
                        .detail()
        )
                .isEqualTo(
                        "bearer-authentication-failed"
                );

        assertThat(
                audit.getValue()
                        .actorSubject()
        )
                .isNull();

        assertThat(
                audit.getValue()
                        .targetUserId()
        )
                .isNull();
    }

    @Test
    void anonymousFailureRemainsPlain401WithoutOidcAudit()
            throws Exception {

        SecurityAuditPort auditPort =
                mock(
                        SecurityAuditPort.class
                );

        AuditingAuthenticationEntryPoint entryPoint =
                new AuditingAuthenticationEntryPoint(
                        auditPort
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException(
                        "authentication required"
                )
        );

        assertThat(
                response.getStatus()
        )
                .isEqualTo(401);

        verifyNoInteractions(
                auditPort
        );
    }
}
