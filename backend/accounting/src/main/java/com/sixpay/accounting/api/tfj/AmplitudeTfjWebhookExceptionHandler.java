package com.sixpay.accounting.api.tfj;

import com.sixpay.accounting.application.exception.TfjConfirmationConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = AmplitudeTfjWebhookController.class
)
public class AmplitudeTfjWebhookExceptionHandler {

    @ExceptionHandler(TfjConfirmationConflictException.class)
    ResponseEntity<ProblemDetail> conflict(
            TfjConfirmationConflictException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );
        problem.setTitle("TFJ confirmation conflict");
        problem.setProperty(
                "code",
                "TFJ_CONFIRMATION_CONFLICT"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<ProblemDetail> unauthorized(
            SecurityException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED,
                        "TFJ webhook authentication failed"
                );
        problem.setTitle("Unauthorized");
        problem.setProperty(
                "code",
                "TFJ_WEBHOOK_AUTHENTICATION_FAILED"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> unprocessable(
            IllegalArgumentException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        exception.getMessage()
                );
        problem.setTitle("Invalid TFJ confirmation");
        problem.setProperty(
                "code",
                "TFJ_CONFIRMATION_INVALID"
        );
        return ResponseEntity.unprocessableEntity()
                .body(problem);
    }
}
