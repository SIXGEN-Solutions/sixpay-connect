package com.sixpay.payment.infrastructure.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Metrics, tracing and structured lifecycle logs around focused Payment
 * application services.
 */
@Aspect
@Component
@ConditionalOnBean({
        PaymentMetrics.class,
        ObservationRegistry.class
})
public final class PaymentObservabilityAspect {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    PaymentObservabilityAspect.class
            );

    private final PaymentMetrics metrics;
    private final ObservationRegistry observationRegistry;

    public PaymentObservabilityAspect(
            PaymentMetrics metrics,
            ObservationRegistry observationRegistry
    ) {
        this.metrics = Objects.requireNonNull(
                metrics,
                "Payment metrics"
        );
        this.observationRegistry = Objects.requireNonNull(
                observationRegistry,
                "Observation registry"
        );
    }

    @Around(
            "execution(public * "
                    + "com.sixpay.payment.application.service."
                    + "Payment*Service.*(..))"
                    + " && !within("
                    + "com.sixpay.payment.application.service."
                    + "PaymentMutationCoordinator)"
    )
    public Object observe(ProceedingJoinPoint joinPoint)
            throws Throwable {

        String operation =
                joinPoint.getSignature()
                        .getDeclaringType()
                        .getSimpleName()
                        + "."
                        + joinPoint.getSignature().getName();

        Observation observation = Observation
                .createNotStarted(
                        "sixpay.payment.operation",
                        observationRegistry
                )
                .lowCardinalityKeyValue(
                        "payment.operation",
                        operation
                );

        long startedAt = System.nanoTime();

        try (Observation.Scope ignored =
                     observation.openScope()) {

            observation.start();

            LOGGER.info(
                    "payment_operation_started operation={}",
                    operation
            );

            Object result = joinPoint.proceed();

            observation.lowCardinalityKeyValue(
                    "payment.outcome",
                    "success"
            );

            metrics.record(
                    operation,
                    "success",
                    elapsed(startedAt)
            );

            LOGGER.info(
                    "payment_operation_completed operation={} "
                            + "outcome=success",
                    operation
            );

            return result;
        } catch (Throwable failure) {
            observation.error(failure);
            observation.lowCardinalityKeyValue(
                    "payment.outcome",
                    "failure"
            );

            metrics.record(
                    operation,
                    "failure",
                    elapsed(startedAt)
            );

            LOGGER.warn(
                    "payment_operation_completed operation={} "
                            + "outcome=failure errorType={}",
                    operation,
                    failure.getClass().getSimpleName()
            );

            throw failure;
        } finally {
            observation.stop();
        }
    }

    private static Duration elapsed(long startedAt) {
        return Duration.ofNanos(
                System.nanoTime() - startedAt
        );
    }
}
