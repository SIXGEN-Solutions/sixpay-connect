package com.sixpay.payment.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "sixpay.payment.initiation")
public record PaymentInitiationProperties(Duration deadline) {
    private static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(30);
    public PaymentInitiationProperties {
        deadline = deadline == null ? DEFAULT_DEADLINE : deadline;
        if (deadline.isZero() || deadline.isNegative()) {
            throw new IllegalArgumentException("sixpay.payment.initiation.deadline must be positive");
        }
    }
}
