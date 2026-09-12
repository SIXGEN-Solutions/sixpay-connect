package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.AccountingT1OperationalCandidateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AccountingT1OperationalQueryController.class)
public class AccountingT1OperationalQueryExceptionHandler {

    @ExceptionHandler(AccountingT1OperationalCandidateNotFoundException.class)
    ProblemDetail notFound(AccountingT1OperationalCandidateNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Accounting T1 operational candidate not found");
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
