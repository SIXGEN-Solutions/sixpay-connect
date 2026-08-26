package com.sixpay.security.api.error;

import com.sixpay.security.api.controller.LocalPasswordController;
import com.sixpay.security.application.exception.CurrentPasswordMismatchException;
import com.sixpay.security.application.exception.PasswordReuseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * HTTP mapping scoped to the user-owned LOCAL password endpoint.
 */
@RestControllerAdvice(
        assignableTypes = LocalPasswordController.class
)
public final class LocalPasswordChangeExceptionHandler {

    @ExceptionHandler(
            CurrentPasswordMismatchException.class
    )
    ProblemDetail handleCurrentPasswordMismatch(
            CurrentPasswordMismatchException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );
        problem.setTitle(
                "Password change rejected"
        );
        problem.setDetail(
                "Current password is invalid"
        );
        return problem;
    }

    @ExceptionHandler(
            PasswordReuseException.class
    )
    ProblemDetail handlePasswordReuse(
            PasswordReuseException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );
        problem.setTitle(
                "Password change rejected"
        );
        problem.setDetail(
                exception.getMessage()
        );
        return problem;
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    ProblemDetail handlePolicyViolation(
            IllegalArgumentException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.BAD_REQUEST
                );
        problem.setTitle(
                "Password policy violation"
        );
        problem.setDetail(
                exception.getMessage()
        );
        return problem;
    }

    @ExceptionHandler(
            IllegalStateException.class
    )
    ProblemDetail handleCredentialUnavailable(
            IllegalStateException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.CONFLICT
                );
        problem.setTitle(
                "Local credential unavailable"
        );
        problem.setDetail(
                "The authenticated account cannot change a LOCAL password"
        );
        return problem;
    }
}
