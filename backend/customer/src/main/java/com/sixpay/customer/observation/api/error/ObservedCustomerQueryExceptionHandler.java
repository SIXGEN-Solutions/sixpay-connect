package com.sixpay.customer.observation.api.error;

import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryRateLimitExceededException;
import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation
        .MethodArgumentTypeMismatchException;

@RestControllerAdvice(
        basePackages =
                "com.sixpay.customer.observation.api"
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
        return problem(
                HttpStatus.NOT_FOUND,
                "Observed Customer not found",
                "Observed Customer was not found",
                "OBSERVED_CUSTOMER_NOT_FOUND"
        );
    }

    @ExceptionHandler({
            InvalidObservedCustomerCursorException.class,
            IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class
    })
    ProblemDetail handleBadRequest(
            Exception exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Observed Customer query",
                invalidDetail(exception),
                "OBSERVED_CUSTOMER_QUERY_INVALID"
        );
    }

    @ExceptionHandler(
            ObservedCustomerQueryRateLimitExceededException.class
    )
    ProblemDetail handleRateLimited(
            ObservedCustomerQueryRateLimitExceededException exception
    ) {
        return problem(
                HttpStatus.TOO_MANY_REQUESTS,
                "Observed Customer query rate limited",
                "Observed Customer query rate limit exceeded",
                "OBSERVED_CUSTOMER_QUERY_RATE_LIMITED"
        );
    }

    @ExceptionHandler(
            ObservedCustomerQueryUnavailableException.class
    )
    ProblemDetail handleUnavailable(
            ObservedCustomerQueryUnavailableException exception
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Observed Customer query unavailable",
                "Observed Customer query is temporarily unavailable",
                "OBSERVED_CUSTOMER_QUERY_UNAVAILABLE"
        );
    }

    @ExceptionHandler(RuntimeException.class)
    ProblemDetail handleInternal(
            RuntimeException exception
    ) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Observed Customer query failed",
                "An internal error occurred",
                "OBSERVED_CUSTOMER_QUERY_INTERNAL_ERROR"
        );
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String code
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setTitle(title);
        problem.setProperty("code", code);

        return problem;
    }

    private static String invalidDetail(
            Exception exception
    ) {
        if (exception
                instanceof MissingRequestHeaderException) {
            return "X-Correlation-ID header is required";
        }

        if (exception
                instanceof MethodArgumentTypeMismatchException mismatch) {
            String name = mismatch.getName();

            if ("observedCustomerId".equals(name)) {
                return "observedCustomerId must be a valid UUID";
            }

            return "Query parameter is invalid";
        }

        if (exception
                instanceof InvalidObservedCustomerCursorException) {
            return "Cursor is invalid";
        }

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "Query parameters are invalid";
        }

        return safeInvalidMessage(message);
    }

    private static String safeInvalidMessage(
            String message
    ) {
        if (message.contains("cursor")) {
            return "Cursor is invalid";
        }

        if (message.contains("UUID")
                || message.contains("identifier")) {
            return "Identifier is invalid";
        }

        return "Query parameters are invalid";
    }
}
