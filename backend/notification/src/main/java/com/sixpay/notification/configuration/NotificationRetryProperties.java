package com.sixpay.notification.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("sixpay.notification.retry")
public class NotificationRetryProperties {

    private boolean enabled = true;
    private int maxAttempts = 5;
    private Duration initialDelay = Duration.ofMinutes(1);
    private double multiplier = 2.0;
    private Duration maxDelay = Duration.ofMinutes(15);
    private Duration schedulerDelay = Duration.ofSeconds(10);
    private int batchSize = 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
    }

    public Duration getSchedulerDelay() {
        return schedulerDelay;
    }

    public void setSchedulerDelay(Duration schedulerDelay) {
        this.schedulerDelay = schedulerDelay;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
