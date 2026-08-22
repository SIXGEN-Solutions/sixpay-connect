package com.sixpay.customer.management.api;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = CustomerController.class
)
public class CustomerApiExceptionHandler {

    @ExceptionHandler(CustomerDomainException.class)
    ProblemDetail domain(CustomerDomainException exception) {
        String message = exception.getMessage();

        HttpStatus status =
                message != null
                        && message.startsWith("customer not found")
                        ? HttpStatus.NOT_FOUND
                        : HttpStatus.CONFLICT;

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        status,
                        message == null
                                ? "Customer operation failed"
                                : message
                );
        detail.setTitle("Customer management error");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );
        detail.setTitle("Invalid customer request");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(
            MethodArgumentNotValidException exception
    ) {
        String detailMessage =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Request validation failed");

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        detailMessage
                );
        detail.setTitle("Invalid customer request");
        return detail;
    }
}
