package com.sixpay.customer.management.api;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = ObservedCustomerLinkController.class
)
public class ObservedCustomerLinkApiExceptionHandler {

    @ExceptionHandler(CustomerDomainException.class)
    ProblemDetail domain(
            CustomerDomainException exception
    ) {
        String message = exception.getMessage();

        HttpStatus status =
                message != null
                        && (
                        message.startsWith("customer not found")
                                || message.startsWith(
                                "observed customer link not found"
                        )
                )
                        ? HttpStatus.NOT_FOUND
                        : HttpStatus.CONFLICT;

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        status,
                        message == null
                                ? "Observed Customer link operation failed"
                                : message
                );

        detail.setTitle(
                "Observed Customer linking error"
        );

        return detail;
    }
}
