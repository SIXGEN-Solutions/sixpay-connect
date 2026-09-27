package com.sixpay.security.api.error;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class LdapAuthenticationExceptionHandler {

    @ExceptionHandler(LdapAuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailure(
            LdapAuthenticationFailedException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        problem.setTitle("Authentication failed");
        problem.setDetail("Invalid credentials");

        return problem;
    }
}
