package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.configuration.PaymentConfigurationParameterPort;

import java.time.Instant;
import java.util.Objects;

/** Resolve only client-owned, administration-approved parameters. */
public class PaymentEventContextService {
    public static final String OPERATION_CODE = "OPERATION_CODE";
    public static final String TECHNICAL_USER = "TECHNICAL_USER";

    private final PaymentConfigurationParameterPort parameterPort;

    public PaymentEventContextService(PaymentConfigurationParameterPort parameterPort) {
        this.parameterPort = Objects.requireNonNull(parameterPort);
    }

    public PaymentEventExecutionPort.PaymentEventMappingContext resolve(Instant requestedAt) {
        return new PaymentEventExecutionPort.PaymentEventMappingContext(
                parameterPort.requireValue(OPERATION_CODE),
                parameterPort.requireValue(TECHNICAL_USER),
                Objects.requireNonNull(requestedAt, "Requested at")
        );
    }
}
