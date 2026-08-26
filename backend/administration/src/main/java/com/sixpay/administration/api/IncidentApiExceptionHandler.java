package com.sixpay.administration.api;

import com.sixpay.administration.domain.exception.IncidentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = IncidentQueryController.class
)
public class IncidentApiExceptionHandler {

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
