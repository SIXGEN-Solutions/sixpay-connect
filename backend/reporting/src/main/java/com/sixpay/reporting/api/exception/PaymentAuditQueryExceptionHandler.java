package com.sixpay.reporting.api.exception;

import com.sixpay.reporting.application.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.UUID;

@RestControllerAdvice(basePackages = "com.sixpay.reporting.api")
public final class PaymentAuditQueryExceptionHandler {

    private static final String CORRELATION = "X-Correlation-ID";

    @ExceptionHandler({
            PaymentAuditNotFoundException.class,
            AuditExportNotFoundException.class
    })
    ResponseEntity<ProblemDetail> notFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Payment audit evidence not found",
                "Payment audit evidence was not found",
                "PAYMENT_AUDIT_NOT_FOUND",
                request,
                null
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ProblemDetail> unauthorized(
            RuntimeException exception,
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
    ResponseEntity<ProblemDetail> forbidden(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Payment audit access denied",
                "Payment audit evidence is not accessible",
                "PAYMENT_AUDIT_FORBIDDEN",
                request,
                null
        );
    }

    @ExceptionHandler(AuditExportConflictException.class)
    ResponseEntity<ProblemDetail> conflict(
            AuditExportConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Audit export idempotency conflict",
                "Idempotency-Key was reused with a different request",
                "PAYMENT_AUDIT_EXPORT_IDEMPOTENCY_CONFLICT",
                request,
                null
        );
    }

    @ExceptionHandler(AuditExportPolicyException.class)
    ResponseEntity<ProblemDetail> unprocessable(
            AuditExportPolicyException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Audit export rejected",
                "Audit export policy rejected the request",
                "PAYMENT_AUDIT_EXPORT_REJECTED",
                request,
                null
        );
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ProblemDetail> badRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid Payment audit query",
                "Payment audit query parameters are invalid",
                "PAYMENT_AUDIT_QUERY_INVALID",
                request,
                null
        );
    }

    @ExceptionHandler(PaymentAuditQueryUnavailableException.class)
    ResponseEntity<ProblemDetail> unavailable(
            PaymentAuditQueryUnavailableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Payment audit query unavailable",
                "Payment audit store is temporarily unavailable",
                "PAYMENT_AUDIT_QUERY_UNAVAILABLE",
                request,
                5
        );
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ProblemDetail> internal(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Payment audit query failed",
                "An internal audit query error occurred",
                "PAYMENT_AUDIT_QUERY_INTERNAL_ERROR",
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
            Integer retryAfter
    ) {
        UUID correlationId = correlation(request);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(
                URI.create(
                        "urn:sixpay:problem:"
                                + code.toLowerCase()
                )
        );
        problem.setProperty("code", code);
        problem.setProperty(
                "correlationId",
                correlationId.toString()
        );

        ResponseEntity.BodyBuilder builder =
                ResponseEntity.status(status)
                        .contentType(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                        .header(
                                CORRELATION,
                                correlationId.toString()
                        );

        if (retryAfter != null) {
            builder.header(
                    HttpHeaders.RETRY_AFTER,
                    retryAfter.toString()
            );
        }

        return builder.body(problem);
    }

    private static UUID correlation(HttpServletRequest request) {
        String value = request.getHeader(CORRELATION);
        if (value != null && !value.isBlank()) {
            try {
                return UUID.fromString(value.strip());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.randomUUID();
    }
}
