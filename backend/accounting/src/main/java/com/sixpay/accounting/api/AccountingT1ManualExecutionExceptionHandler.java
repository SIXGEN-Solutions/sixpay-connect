package com.sixpay.accounting.api;

import com.sixpay.accounting.application.exception.AccountingProviderAuthenticationException;
import com.sixpay.accounting.application.exception.AccountingProviderInvalidResponseException;
import com.sixpay.accounting.application.exception.AccountingProviderUnavailableException;
import com.sixpay.accounting.application.exception.AccountingT1ManualExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = AccountingT1ManualExecutionController.class
)
public class AccountingT1ManualExecutionExceptionHandler {

    @ExceptionHandler(AccountingT1ManualExecutionException.class)
    ResponseEntity<ProblemDetail> handleManualExecution(
            AccountingT1ManualExecutionException exception
    ) {
        HttpStatus status = switch (exception.reason()) {
            case NO_CANDIDATE_OR_BATCH -> HttpStatus.UNPROCESSABLE_ENTITY;
            case MULTIPLE_FINANCIAL_INSTITUTIONS -> HttpStatus.CONFLICT;
        };
        return problem(status, exception.getMessage());
    }

    @ExceptionHandler({
            AccountingProviderAuthenticationException.class,
            AccountingProviderInvalidResponseException.class
    })
    ResponseEntity<ProblemDetail> handleBadGateway(
            RuntimeException exception
    ) {
        return problem(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccountingProviderUnavailableException.class)
    ResponseEntity<ProblemDetail> handleUnavailable(
            AccountingProviderUnavailableException exception
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(
                "Accounting T1 manual execution failed"
        );
        return ResponseEntity.status(status).body(problem);
    }
}
