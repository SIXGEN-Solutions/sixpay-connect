package com.sixpay.customer.observation.api.error;

import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryRateLimitExceededException;
import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication
        .AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation
        .MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.UUID;

@RestControllerAdvice(
        basePackages =
                "com.sixpay.customer.observation.api"
)
public final class ObservedCustomerQueryExceptionHandler {

    private static final String CORRELATION_HEADER =
            "X-Correlation-ID";

    @ExceptionHandler(
            com.sixpay.customer.observation.application.exception
                    .ObservedCustomerNotFoundException.class
    )
    ResponseEntity<ProblemDetail> handleNotFound(
            com.sixpay.customer.observation.application.exception
                    .ObservedCustomerNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Observed Customer not found",
                "Observed Customer was not found",
                "OBSERVED_CUSTOMER_NOT_FOUND",
                request,
                null
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ProblemDetail> handleUnauthorized(
            AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "Authentication is required",
                "AUTHENTICATION_REQUIRED",
                request,
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleForbidden(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Observed Customer access denied",
                "Observed Customer is not accessible",
                "OBSERVED_CUSTOMER_ACCESS_DENIED",
                request,
                null
        );
    }

    @ExceptionHandler({
            InvalidObservedCustomerCursorException.class,
            IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ProblemDetail> handleBadRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Observed Customer query",
                invalidDetail(exception),
                "OBSERVED_CUSTOMER_QUERY_INVALID",
                request,
                null
        );
    }

    @ExceptionHandler(
            ObservedCustomerQueryRateLimitExceededException.class
    )
    ResponseEntity<ProblemDetail> handleRateLimited(
            ObservedCustomerQueryRateLimitExceededException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.TOO_MANY_REQUESTS,
                "Observed Customer query rate limited",
                "Observed Customer query rate limit exceeded",
                "OBSERVED_CUSTOMER_QUERY_RATE_LIMITED",
                request,
                1
        );
    }

    @ExceptionHandler(
            ObservedCustomerQueryUnavailableException.class
    )
    ResponseEntity<ProblemDetail> handleUnavailable(
            ObservedCustomerQueryUnavailableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Observed Customer query unavailable",
                "Observed Customer query is temporarily unavailable",
                "OBSERVED_CUSTOMER_QUERY_UNAVAILABLE",
                request,
                5
        );
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ProblemDetail> handleInternal(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Observed Customer query failed",
                "An internal error occurred",
                "OBSERVED_CUSTOMER_QUERY_INTERNAL_ERROR",
                request,
                null
        );
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code,
            HttpServletRequest request,
            Integer retryAfterSeconds
    ) {
        UUID correlationId = correlationId(request);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setType(
                URI.create(
                        "urn:sixpay:problem:"
                                + code.toLowerCase()
                )
        );
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty(
                "correlationId",
                correlationId.toString()
        );

        ResponseEntity.BodyBuilder response =
                ResponseEntity.status(status)
                        .contentType(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                        .header(
                                CORRELATION_HEADER,
                                correlationId.toString()
                        );

        if (retryAfterSeconds != null) {
            response.header(
                    HttpHeaders.RETRY_AFTER,
                    retryAfterSeconds.toString()
            );
        }

        return response.body(problem);
    }

    private static UUID correlationId(
            HttpServletRequest request
    ) {
        String value = request.getHeader(CORRELATION_HEADER);

        if (value != null && !value.isBlank()) {
            try {
                return UUID.fromString(value.strip());
            } catch (IllegalArgumentException ignored) {
                // Invalid input gets a server-issued correlation identifier.
            }
        }

        return UUID.randomUUID();
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
