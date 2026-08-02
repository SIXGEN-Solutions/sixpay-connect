package com.sixpay.payment.api;

import com.sixpay.payment.api.response.PaymentProblemResponse;
import com.sixpay.payment.application.security.PaymentAccessDeniedException;
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

@RestControllerAdvice
public class PaymentApiExceptionHandler {

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
                "INVALID_PAYMENT_REQUEST",
                exception.getMessage(),
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
        var response = new PaymentProblemResponse(
                URI.create("urn:sixpay:problem:" + code.toLowerCase()),
                status.getReasonPhrase(),
                status.value(),
                code,
                correlationId,
                detail,
                null,
                List.of()
        );

        return ResponseEntity.status(status)
                .header("X-Correlation-ID", correlationId.toString())
                .body(response);
    }

    private static UUID correlationId(HttpServletRequest request) {
        try {
            return UUID.fromString(request.getHeader("X-Correlation-ID"));
        } catch (RuntimeException exception) {
            return UUID.randomUUID();
        }
    }
}
