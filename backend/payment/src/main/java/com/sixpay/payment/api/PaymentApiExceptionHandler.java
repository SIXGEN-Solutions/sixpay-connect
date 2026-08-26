package com.sixpay.payment.api;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.api.response.PaymentProblemResponse;
import com.sixpay.payment.application.exception.PaymentQueryUnavailableException;
import com.sixpay.payment.application.security.PaymentAccessDeniedException;
import com.sixpay.payment.infrastructure.idempotency.PaymentIdempotencyConflictException;
import com.sixpay.payment.infrastructure.tresorpay.TresorPayRequestRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class PaymentApiExceptionHandler {

    @ExceptionHandler(TresorPayRequestRejectedException.class)
    ResponseEntity<PaymentProblemResponse> tresorPayRejected(
            TresorPayRequestRejectedException exception,
            HttpServletRequest request
    ) {
        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(exception.status());

        if (exception.retryAfterSeconds() != null) {
            builder.header(
                    "Retry-After",
                    exception.retryAfterSeconds().toString()
            );
        }

        UUID correlationId = correlationId(request);

        return builder
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.toString()
                )
                .body(problemBody(
                        exception.status(),
                        exception.code().name(),
                        exception.getMessage(),
                        correlationId,
                        exception.retryAfterSeconds()
                ));
    }

    @ExceptionHandler(PaymentIdempotencyConflictException.class)
    ResponseEntity<PaymentProblemResponse> idempotencyConflict(
            PaymentIdempotencyConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_KEY_CONFLICT",
                "Idempotency key is already associated with another request",
                request
        );
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<PaymentProblemResponse> notFound(
            PaymentNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(PaymentAccessDeniedException.class)
    ResponseEntity<PaymentProblemResponse> forbidden(
            PaymentAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "PAYMENT_ACCESS_DENIED",
                "Payment is not accessible",
                request
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<PaymentProblemResponse> unauthorized(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required",
                request
        );
    }

    @ExceptionHandler(PaymentQueryRateLimitExceededException.class)
    ResponseEntity<PaymentProblemResponse> rateLimited(
            PaymentQueryRateLimitExceededException exception,
            HttpServletRequest request
    ) {
        UUID correlationId = correlationId(request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header("Retry-After", Integer.toString(exception.retryAfterSeconds()))
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.toString()
                )
                .body(problemBody(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "PAYMENT_QUERY_RATE_LIMITED",
                        "Payment query rate limit exceeded",
                        correlationId,
                        exception.retryAfterSeconds()
                ));
    }

    @ExceptionHandler(PaymentQueryUnavailableException.class)
    ResponseEntity<PaymentProblemResponse> unavailable(
            PaymentQueryUnavailableException exception,
            HttpServletRequest request
    ) {
        UUID correlationId = correlationId(request);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header("Retry-After", "5")
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.toString()
                )
                .body(problemBody(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "PAYMENT_QUERY_UNAVAILABLE",
                        "Payment query projection is temporarily unavailable",
                        correlationId,
                        5
                ));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<PaymentProblemResponse> badRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Payment request is invalid",
                request
        );
    }

    private static ResponseEntity<PaymentProblemResponse> problem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        UUID correlationId = correlationId(request);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.toString()
                )
                .body(problemBody(
                        status,
                        code,
                        detail,
                        correlationId,
                        null
                ));
    }

    private static PaymentProblemResponse problemBody(
            HttpStatus status,
            String code,
            String detail,
            UUID correlationId,
            Integer retryAfter
    ) {
        return new PaymentProblemResponse(
                URI.create(
                        "urn:sixpay:problem:"
                                + code.toLowerCase()
                ),
                status.getReasonPhrase(),
                status.value(),
                code,
                correlationId,
                detail,
                retryAfter,
                List.of()
        );
    }

    private static UUID correlationId(
            HttpServletRequest request
    ) {
        try {
            return UUID.fromString(
                    request.getHeader(
                            IntegrationHttpHeaders.CORRELATION_ID
                    )
            );
        } catch (RuntimeException exception) {
            return UUID.randomUUID();
        }
    }
}
