package com.sixpay.payment.infrastructure.web;

import com.sixpay.payment.infrastructure.web.dto.PaymentProblemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    ResponseEntity<PaymentProblemResponse> notFound(PaymentProjectionNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage(), correlationId(request));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class, MissingRequestHeaderException.class, IllegalArgumentException.class})
    ResponseEntity<PaymentProblemResponse> badRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_QUERY", ex.getMessage(), correlationId(request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<PaymentProblemResponse> internal(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_QUERY_FAILURE", "Internal Payment query failure", correlationId(request));
    }

    private static ResponseEntity<PaymentProblemResponse> problem(HttpStatus status, String code, String detail, UUID correlationId) {
        var body = new PaymentProblemResponse(
                URI.create("urn:sixpay:problem:" + code.toLowerCase()),
                status.getReasonPhrase(), status.value(), code,
                correlationId, detail, null, List.of()
        );
        return ResponseEntity.status(status)
                .header("X-Correlation-ID", correlationId.toString())
                .body(body);
    }

    private static UUID correlationId(HttpServletRequest request) {
        String value = request.getHeader("X-Correlation-ID");
        try { return value == null ? UUID.randomUUID() : UUID.fromString(value); }
        catch (IllegalArgumentException ex) { return UUID.randomUUID(); }
    }
}
