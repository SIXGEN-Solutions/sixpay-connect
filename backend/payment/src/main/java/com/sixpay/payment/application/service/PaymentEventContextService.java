package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentEventContextPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.configuration.PaymentConfigurationParameterPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class PaymentEventContextService {

    public static final String OPERATION_CODE = "OPERATION_CODE";
    public static final String TECHNICAL_USER = "TECHNICAL_USER";

    private final PaymentConfigurationParameterPort parameterPort;
    private final PaymentEventContextPort contextPort;

    public PaymentEventContextService(
            PaymentConfigurationParameterPort parameterPort,
            PaymentEventContextPort contextPort
    ) {
        this.parameterPort = Objects.requireNonNull(parameterPort);
        this.contextPort = Objects.requireNonNull(contextPort);
    }

    public PaymentEventExecutionPort.PaymentEventMappingContext resolve(
            BankingRequestContext bankingContext,
            Instant requestedAt
    ) {
        Objects.requireNonNull(bankingContext, "Banking context");
        Objects.requireNonNull(requestedAt, "Requested at");

        String operationCode =
                parameterPort.requireValue(OPERATION_CODE);
        String technicalUser =
                parameterPort.requireValue(TECHNICAL_USER);

        long eventNumber =
                contextPort.allocateNextEventNumber(
                        operationCode,
                        bankingContext
                );

        return new PaymentEventExecutionPort.PaymentEventMappingContext(
                operationCode,
                eventNumber,
                contextPort.getAccountingDate(bankingContext),
                contextPort.getNightMode(bankingContext),
                technicalUser,
                requestedAt
        );
    }
}
