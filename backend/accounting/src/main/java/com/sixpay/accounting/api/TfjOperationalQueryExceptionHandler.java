package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.TfjOperationalConfirmationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TfjOperationalQueryController.class)
public class TfjOperationalQueryExceptionHandler {

    @ExceptionHandler(TfjOperationalConfirmationNotFoundException.class)
    ProblemDetail handleNotFound(
            TfjOperationalConfirmationNotFoundException exception
    ) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("TFJ operational confirmation not found");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
