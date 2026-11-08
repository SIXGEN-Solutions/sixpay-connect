package com.sixpay.payment.infrastructure.recovery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "sixpay.payment.t0-recovery")
public class PaymentT0RecoveryBatchProperties {
    private boolean enabled;
    private int batchSize = 50;
    private Duration window = Duration.ofHours(48);
    private Duration minimumAge = Duration.ofMinutes(2);
    private Duration claimTimeout = Duration.ofMinutes(5);
    private String workerId = "payment-t0-recovery-" + java.util.UUID.randomUUID();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int value) { batchSize = value; }
    public Duration getWindow() { return window; }
    public void setWindow(Duration value) { window = value; }
    public Duration getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Duration value) { minimumAge = value; }
    public Duration getClaimTimeout() { return claimTimeout; }
    public void setClaimTimeout(Duration value) { claimTimeout = value; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String value) { workerId = value; }

    public void validate() {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalStateException("T0 recovery batchSize must be between 1 and 500");
        }
        if (window == null || window.isNegative() || window.isZero()
                || minimumAge == null || minimumAge.isNegative()
                || minimumAge.compareTo(window) >= 0) {
            throw new IllegalStateException("Invalid T0 recovery window / minimumAge");
        }
        if (claimTimeout == null || claimTimeout.isNegative() || claimTimeout.isZero()) {
            throw new IllegalStateException("T0 recovery claimTimeout must be positive");
        }
        if (workerId == null || workerId.isBlank() || workerId.length() > 150) {
            throw new IllegalStateException("T0 recovery workerId is required (max 150 characters)");
        }
    }
}
