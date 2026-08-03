package com.sixpay.partner.infrastructure.observability;

import com.sixpay.partner.application.port.output.PartnerOperationMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerPartnerOperationMetrics implements PartnerOperationMetrics {

    private static final String METRIC_NAME = "sixpay.partner.operations";

    private final MeterRegistry meterRegistry;

    public MicrometerPartnerOperationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void succeeded(Operation operation) {
        increment(operation.name(), "success");
    }

    @Override
    public void replayed(Operation operation) {
        increment(operation.name(), "replay");
    }

    @Override
    public void rejected(Rejection rejection) {
        increment("REQUEST", "rejected_" + rejection.name().toLowerCase(java.util.Locale.ROOT));
    }

    private void increment(String operation, String outcome) {
        meterRegistry.counter(
                METRIC_NAME,
                "operation", operation,
                "outcome", outcome
        ).increment();
    }
}
