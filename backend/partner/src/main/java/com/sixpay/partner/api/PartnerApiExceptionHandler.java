package com.sixpay.partner.api;

import com.sixpay.partner.application.exception.PartnerNotFoundException;
import com.sixpay.partner.application.port.output.PartnerOperationMetrics;
import com.sixpay.partner.domain.exception.PartnerDomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.net.URI;
import java.util.LinkedHashMap;

@RestControllerAdvice(assignableTypes = PartnerController.class)
public class PartnerApiExceptionHandler {

    private final PartnerOperationMetrics metrics;

    public PartnerApiExceptionHandler(PartnerOperationMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(PartnerNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(PartnerNotFoundException exception, HttpServletRequest request) {
        metrics.rejected(PartnerOperationMetrics.Rejection.NOT_FOUND);
        return problem(HttpStatus.NOT_FOUND, "Partner not found", exception.getMessage(), request);
    }

    @ExceptionHandler({PartnerDomainException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> businessRule(RuntimeException exception, HttpServletRequest request) {
        metrics.rejected(PartnerOperationMetrics.Rejection.DOMAIN_RULE);
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Partner operation rejected", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        metrics.rejected(PartnerOperationMetrics.Rejection.INVALID_REQUEST);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        detail.setTitle("Invalid request");
        detail.setType(URI.create("urn:sixpay:problem:invalid-request"));
        detail.setInstance(URI.create(request.getRequestURI()));
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler({
            MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class
            //HandlerMethodValidationException.class
    })
    ResponseEntity<ProblemDetail> malformedRequest(Exception exception, HttpServletRequest request) {
        metrics.rejected(PartnerOperationMetrics.Rejection.INVALID_REQUEST);
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Request is missing required data or contains malformed data",
                request
        );
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class
    })
    ResponseEntity<ProblemDetail> conflict(RuntimeException exception, HttpServletRequest request) {
        metrics.rejected(PartnerOperationMetrics.Rejection.DOMAIN_RULE);
        return problem(
                HttpStatus.CONFLICT,
                "Concurrent or duplicate operation",
                "The operation conflicts with the current partner state",
                request
        );
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:sixpay:problem:" + status.value()));
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> methodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        metrics.rejected(PartnerOperationMetrics.Rejection.INVALID_REQUEST);

        var detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );

        detail.setTitle("Invalid request");
        detail.setType(URI.create("urn:sixpay:problem:invalid-request"));
        detail.setInstance(URI.create(request.getRequestURI()));

        var errors = new LinkedHashMap<String, String>();

        exception.getParameterValidationResults()
                .forEach(result -> {
                    if (result instanceof ParameterErrors parameterErrors) {
                        parameterErrors.getFieldErrors()
                                .forEach(error -> errors.putIfAbsent(
                                        error.getField(),
                                        error.getDefaultMessage()
                                ));
                    }
                });

        detail.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(detail);
    }
}
