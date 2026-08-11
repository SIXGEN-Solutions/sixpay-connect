package com.sixpay.security.api.error;

import com.sixpay.security.application.exception.LocalAuthenticationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class LocalAuthenticationExceptionHandler {

    @ExceptionHandler(LocalAuthenticationFailedException.class)
    ProblemDetail handleAuthenticationFailure(
            LocalAuthenticationFailedException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);

        problem.setTitle("Authentication failed");
        problem.setDetail("Invalid credentials");

        return problem;
    }
}
