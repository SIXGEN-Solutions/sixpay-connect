package com.sixpay.payment.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class PaymentInitiationDeadline {
    private final Duration duration;
    public PaymentInitiationDeadline(Duration duration) {
        this.duration=Objects.requireNonNull(duration,"Initiation deadline");
        if(duration.isZero()||duration.isNegative()) throw new IllegalArgumentException("Initiation deadline must be positive");
    }
    public boolean expired(Instant receivedAt,Instant observedAt) {
        Objects.requireNonNull(receivedAt,"Received instant");
        Objects.requireNonNull(observedAt,"Observed instant");
        return !observedAt.isBefore(receivedAt.plus(duration));
    }
    public Instant deadlineAt(Instant receivedAt) {
        Objects.requireNonNull(receivedAt, "Received instant");
        return receivedAt.plus(duration);
    }
    public Duration duration(){ return duration; }
}
