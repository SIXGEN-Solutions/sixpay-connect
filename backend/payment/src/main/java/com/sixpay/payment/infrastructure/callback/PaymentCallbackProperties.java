package com.sixpay.payment.infrastructure.callback;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "sixpay.payment.callback")
public class PaymentCallbackProperties {

    private boolean enabled;
    private int batchSize = 50;
    private int maxAttempts = 8;
    private Duration claimTimeout = Duration.ofMinutes(5);
    private Duration initialRetryDelay = Duration.ofSeconds(10);
    private Duration maximumRetryDelay = Duration.ofHours(1);
    private String workerId = "payment-callback-worker";
    private String signingKeyId;
    private String signingPrivateKeyPem;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getClaimTimeout() {
        return claimTimeout;
    }

    public void setClaimTimeout(Duration claimTimeout) {
        this.claimTimeout = claimTimeout;
    }

    public Duration getInitialRetryDelay() {
        return initialRetryDelay;
    }

    public void setInitialRetryDelay(Duration initialRetryDelay) {
        this.initialRetryDelay = initialRetryDelay;
    }

    public Duration getMaximumRetryDelay() {
        return maximumRetryDelay;
    }

    public void setMaximumRetryDelay(Duration maximumRetryDelay) {
        this.maximumRetryDelay = maximumRetryDelay;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getSigningKeyId() {
        return signingKeyId;
    }

    public void setSigningKeyId(String signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    public String getSigningPrivateKeyPem() {
        return signingPrivateKeyPem;
    }

    public void setSigningPrivateKeyPem(
            String signingPrivateKeyPem
    ) {
        this.signingPrivateKeyPem = signingPrivateKeyPem;
    }

    public void validateEnabledConfiguration() {
        if (!enabled) {
            return;
        }
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalStateException(
                    "Callback batch size must be between 1 and 500"
            );
        }
        if (maxAttempts < 1) {
            throw new IllegalStateException(
                    "Callback max attempts must be positive"
            );
        }
        if (signingKeyId == null
                || signingKeyId.isBlank()) {
            throw new IllegalStateException(
                    "Callback signing key ID is required"
            );
        }
        if (signingPrivateKeyPem == null
                || signingPrivateKeyPem.isBlank()) {
            throw new IllegalStateException(
                    "Callback signing private key is required"
            );
        }
    }
}
