package com.sixpay.payment.infrastructure.web;

import com.sixpay.payment.application.security.PaymentAccessDeniedException;
import com.sixpay.payment.infrastructure.web.dto.PaymentProblemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice(assignableTypes = PaymentQueryController.class)
public class PaymentRestExceptionHandler {

    @ExceptionHandler(PaymentProjectionNotFoundException.class)
    ResponseEntity<PaymentProblemResponse> notFound(
            PaymentProjectionNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                exception.getMessage(),
                correlationId(request)
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
                correlationId(request)
        );
    }

    @ExceptionHandler(
            AuthenticationCredentialsNotFoundException.class
    )
    ResponseEntity<PaymentProblemResponse> unauthorized(
            AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required",
                correlationId(request)
        );
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
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
                "INVALID_PAYMENT_QUERY",
                exception.getMessage(),
                correlationId(request)
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<PaymentProblemResponse> internalFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PAYMENT_QUERY_FAILURE",
                "Internal Payment query failure",
                correlationId(request)
        );
    }

    private static ResponseEntity<PaymentProblemResponse> problem(
            HttpStatus status,
            String code,
            String detail,
            UUID correlationId
    ) {
        PaymentProblemResponse body =
                new PaymentProblemResponse(
                        URI.create(
                                "urn:sixpay:problem:"
                                        + code.toLowerCase()
                        ),
                        status.getReasonPhrase(),
                        status.value(),
                        code,
                        correlationId,
                        detail,
                        null,
                        List.of()
                );

        return ResponseEntity.status(status)
                .header(
                        "X-Correlation-ID",
                        correlationId.toString()
                )
                .body(body);
    }

    private static UUID correlationId(
            HttpServletRequest request
    ) {
        String value = request.getHeader(
                "X-Correlation-ID"
        );

        try {
            return value == null
                    ? UUID.randomUUID()
                    : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID();
        }
    }
}
