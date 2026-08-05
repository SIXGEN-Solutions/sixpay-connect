package com.sixpay.customer.observation.api.error;

import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.sixpay.customer.observation.api"
)
public final class ObservedCustomerQueryExceptionHandler {

    @ExceptionHandler(
            com.sixpay.customer.observation.application.exception
                    .ObservedCustomerNotFoundException.class
    )
    ProblemDetail handleNotFound(
            com.sixpay.customer.observation.application.exception
                    .ObservedCustomerNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Observed Customer was not found"
        );
        problem.setTitle("Observed Customer not found");
        problem.setProperty(
                "code",
                "OBSERVED_CUSTOMER_NOT_FOUND"
        );
        return problem;
    }

    @ExceptionHandler({
            InvalidObservedCustomerCursorException.class,
            IllegalArgumentException.class
    })
    ProblemDetail handleBadRequest(
            RuntimeException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                safeMessage(exception)
        );
        problem.setTitle("Invalid Observed Customer query");
        problem.setProperty(
                "code",
                "OBSERVED_CUSTOMER_QUERY_INVALID"
        );
        return problem;
    }

    @ExceptionHandler(
            ObservedCustomerQueryUnavailableException.class
    )
    ProblemDetail handleUnavailable(
            ObservedCustomerQueryUnavailableException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Observed Customer query is temporarily unavailable"
        );
        problem.setTitle("Observed Customer query unavailable");
        problem.setProperty(
                "code",
                "OBSERVED_CUSTOMER_QUERY_UNAVAILABLE"
        );
        return problem;
    }

    private static String safeMessage(
            RuntimeException exception
    ) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "The query parameters are invalid";
        }

        return message;
    }
}
