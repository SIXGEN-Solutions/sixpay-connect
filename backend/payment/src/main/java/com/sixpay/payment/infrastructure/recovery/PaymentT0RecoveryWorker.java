package com.sixpay.payment.infrastructure.recovery;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.input.RecoverUnknownPaymentT0UseCase;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Lookup-only scheduler; financial execution POST is never called here. */
@Component
@ConditionalOnProperty(prefix = "sixpay.payment.t0-recovery", name = "enabled", havingValue = "true")
public class PaymentT0RecoveryWorker {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentT0RecoveryWorker.class);
    private static final Duration[] RETRIES = {
            Duration.ofMinutes(2), Duration.ofMinutes(5), Duration.ofMinutes(15),
            Duration.ofMinutes(30), Duration.ofMinutes(60)
    };

    private final PaymentT0RecoveryClaimStore store;
    private final RecoverUnknownPaymentT0UseCase recovery;
    private final PaymentT0RecoveryBatchProperties properties;
    private final TimeProvider time;

    public PaymentT0RecoveryWorker(PaymentT0RecoveryClaimStore store,
            RecoverUnknownPaymentT0UseCase recovery,
            PaymentT0RecoveryBatchProperties properties, TimeProvider time) {
        this.store = Objects.requireNonNull(store);
        this.recovery = Objects.requireNonNull(recovery);
        this.properties = Objects.requireNonNull(properties);
        this.time = Objects.requireNonNull(time);
        properties.validate();
    }

    @Scheduled(fixedDelayString = "${sixpay.payment.t0-recovery.poll-delay:PT2M}")
    public void reconcileAvailable() {
        Instant now = time.now();
        for (PaymentT0RecoveryClaimStore.Claim claim : store.claim(now, properties)) {
            try {
                var result = recovery.recoverByPaymentId(
                        new PaymentId(claim.paymentId()), CorrelationId.generate());
                if (result.status() == PaymentStatus.POSTING_PENDING
                        || result.status() == PaymentStatus.POSTING_OUTCOME_UNKNOWN) {
                    store.retry(claim, time.now(), delay(claim.attempt()));
                } else {
                    store.completed(claim, time.now());
                }
            } catch (Exception exception) {
                // Unavailable or inconsistent bank evidence cannot prove rejection.
                LOG.warn("T0 lookup failed for payment {} (attempt {})",
                        claim.paymentId(), claim.attempt(), exception);
                store.retry(claim, time.now(), delay(claim.attempt()));
            }
        }
    }

    static Duration delay(int attempt) {
        return RETRIES[Math.min(Math.max(1, attempt), RETRIES.length) - 1];
    }
}
