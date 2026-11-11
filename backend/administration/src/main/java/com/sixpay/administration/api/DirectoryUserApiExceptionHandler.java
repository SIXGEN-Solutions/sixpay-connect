package com.sixpay.administration.api;

import com.sixpay.security.application.port.input.DirectoryUnavailableException;
import com.sixpay.security.application.port.input.DirectoryUserAmbiguousException;
import com.sixpay.security.application.port.input.DirectoryUserNotFoundException;
import com.sixpay.security.application.port.input.LdapProvisioningConflictException;
import com.sixpay.security.application.port.input.LdapUserNotProvisionableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DirectoryUserAdministrationController.class)
public class DirectoryUserApiExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    ProblemDetail handleInvalidRequest(Exception exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid directory administration request",
                exception.getMessage()
        );
    }

    @ExceptionHandler(DirectoryUserNotFoundException.class)
    ProblemDetail handleDirectoryUserNotFound(
            DirectoryUserNotFoundException exception
    ) {
        return problem(HttpStatus.NOT_FOUND, "Directory user not found", exception.getMessage());
    }

    @ExceptionHandler(DirectoryUserAmbiguousException.class)
    ProblemDetail handleDirectoryUserAmbiguous(
            DirectoryUserAmbiguousException exception
    ) {
        return problem(HttpStatus.CONFLICT, "Directory user lookup is ambiguous", exception.getMessage());
    }

    @ExceptionHandler(LdapUserNotProvisionableException.class)
    ProblemDetail handleNotProvisionable(
            LdapUserNotProvisionableException exception
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Directory user is not provisionable",
                exception.getMessage()
        );
    }

    @ExceptionHandler(LdapProvisioningConflictException.class)
    ProblemDetail handleProvisioningConflict(
            LdapProvisioningConflictException exception
    ) {
        ProblemDetail detail =
                problem(
                        HttpStatus.CONFLICT,
                        "LDAP provisioning conflict",
                        exception.getMessage()
                );
        detail.setProperty("reason", exception.reason().name());
        return detail;
    }

    @ExceptionHandler(DirectoryUnavailableException.class)
    ProblemDetail handleDirectoryUnavailable(
            DirectoryUnavailableException exception
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Directory service unavailable",
                exception.getMessage()
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ProblemDetail handleUnauthenticated(
            AuthenticationCredentialsNotFoundException exception
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleForbidden(
            AccessDeniedException exception
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                exception.getMessage()
        );
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail == null
                                ? status.getReasonPhrase()
                                : detail
                );
        problem.setTitle(title);
        return problem;
    }
}
