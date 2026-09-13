package com.sixpay.administration.api;

import com.sixpay.administration.domain.exception.IncidentNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = IncidentQueryController.class
)
public class IncidentApiExceptionHandler {

    @ExceptionHandler(
            ConstraintViolationException.class
    )
    ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle(
                "Invalid incident query parameters"
        );

        problem.setProperty(
                "code",
                "INCIDENT_QUERY_INVALID"
        );

        return problem;
    }

    @ExceptionHandler(
            IncidentNotFoundException.class
    )
    ProblemDetail handleNotFound(
            IncidentNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle(
                "Operational incident not found"
        );

        problem.setProperty(
                "code",
                "INCIDENT_NOT_FOUND"
        );

        return problem;
    }
}
