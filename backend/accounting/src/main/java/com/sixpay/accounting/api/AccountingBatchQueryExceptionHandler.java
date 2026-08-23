package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.AccountingBatchNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AccountingBatchQueryController.class)
public class AccountingBatchQueryExceptionHandler {

    @ExceptionHandler(AccountingBatchNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail notFound(
            AccountingBatchNotFoundException exception
    ) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Accounting batch not found");
        detail.setDetail(exception.getMessage());
        return detail;
    }
}
