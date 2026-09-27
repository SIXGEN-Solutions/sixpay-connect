package com.sixpay.security.api.error;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class LdapAuthenticationExceptionHandler {

    /*
     * The public LDAP credential boundary deliberately does not reveal whether
     * credentials were rejected by LDAP, the external identity is not linked,
     * or the canonical SIXPAY account is disabled. These remain distinct
     * application/domain failures internally, but are indistinguishable to
     * the unauthenticated caller to prevent account/link-state enumeration.
     */
    @ExceptionHandler({
            LdapAuthenticationFailedException.class,
            ExternalIdentityNotLinkedException.class,
            SixpayUserDisabledException.class
    })
    ProblemDetail handleAuthenticationFailure(
            RuntimeException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        problem.setTitle("Authentication failed");
        problem.setDetail("Invalid credentials");

        return problem;
    }
}
