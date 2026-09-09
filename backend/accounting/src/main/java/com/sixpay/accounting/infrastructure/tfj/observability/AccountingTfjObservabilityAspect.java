package com.sixpay.accounting.infrastructure.tfj.observability;

import com.sixpay.accounting.application.exception.TfjConfirmationConflictException;
import com.sixpay.accounting.application.service.TfjIngestionResult;
import com.sixpay.accounting.domain.model.TfjConfirmation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Infrastructure-only metrics instrumentation for the TFJ application flow.
 */
@Aspect
@Component
@ConditionalOnBean(AccountingTfjMetrics.class)
public final class AccountingTfjObservabilityAspect {

    private final AccountingTfjMetrics metrics;

    public AccountingTfjObservabilityAspect(
            AccountingTfjMetrics metrics
    ) {
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Around(
            "execution(* com.sixpay.accounting.application.service."
                    + "TfjIngestionService.ingest(..))"
    )
    public Object observeIngestion(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {
        TfjConfirmation confirmation =
                (TfjConfirmation) joinPoint.getArgs()[0];

        try {
            TfjIngestionResult result =
                    (TfjIngestionResult) joinPoint.proceed();
            metrics.recordIngestion(
                    confirmation.status(),
                    result.receiptStatus()
            );
            return result;
        } catch (TfjConfirmationConflictException exception) {
            metrics.recordConflict();
            throw exception;
        }
    }

    @Around(
            "execution(* com.sixpay.accounting.application.service."
                    + "TfjFinalityPublicationService.afterCommit(..))"
    )
    public Object observeAfterCommitPublication(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            metrics.recordFinalityPublication("SUCCESS", 1);
            return result;
        } catch (Throwable throwable) {
            metrics.recordFinalityPublication("FAILED", 1);
            throw throwable;
        }
    }

    @Around(
            "execution(* com.sixpay.accounting.application.service."
                    + "TfjFinalityPublicationService.publishPending(..))"
    )
    public Object observeRecoveryPublication(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {
        try {
            Integer published = (Integer) joinPoint.proceed();
            metrics.recordFinalityPublication(
                    "RECOVERY",
                    published.doubleValue()
            );
            return published;
        } catch (Throwable throwable) {
            metrics.recordFinalityPublication("RECOVERY_FAILED", 1);
            throw throwable;
        }
    }
}
