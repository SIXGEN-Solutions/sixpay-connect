package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcome;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.error.AmplitudePaymentEventOutcomeUnknownException;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper.AmplitudePaymentEventMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper.AmplitudePaymentEventMappingContext;

import java.util.Locale;
import java.util.Objects;

public final class AmplitudePaymentEventAdapter
        implements PaymentEventExecutionPort {

    private static final String PAYMENT_NATURE = "VIRPAY";

    private final AmplitudePaymentEventClient client;
    private final AmplitudePaymentEventMapper mapper;

    public AmplitudePaymentEventAdapter(
            AmplitudePaymentEventClient client,
            AmplitudePaymentEventMapper mapper
    ) {
        this.client = Objects.requireNonNull(client, "Amplitude Payment event client");
        this.mapper = Objects.requireNonNull(mapper, "Amplitude Payment event mapper");
    }

    @Override
    public PaymentEventExecutionResult execute(
            PaymentEventExecutionCommand command
    ) {
        Objects.requireNonNull(command, "Payment event execution command");

        PaymentEventMappingContext context = command.mappingContext();

        try {
            AmplitudePaymentEventResult providerResult =
                    client.execute(
                            mapper.toRequest(
                                    command.snapshot(),
                                    new AmplitudePaymentEventMappingContext(
                                            context.operationCode(),
                                            context.eventNumber(),
                                            context.accountingDate(),
                                            context.nightMode(),
                                            PAYMENT_NATURE,
                                            context.technicalUser(),
                                            context.requestedAt()
                                    )
                            ),
                            command.context().correlationId().value(),
                            command.context().financialInstitutionCode().value(),
                            command.idempotencyKey().value()
                    );

            return toApplicationResult(providerResult);
        } catch (AmplitudePaymentEventOutcomeUnknownException exception) {
            throw new PaymentEventOutcomeUnknownException(
                    exception.getMessage(),
                    exception
            );
        }
    }

    static PaymentEventExecutionResult toApplicationResult(
            AmplitudePaymentEventResult result
    ) {
        Objects.requireNonNull(result, "Amplitude Payment event result");

        return new PaymentEventExecutionResult(
                result.paymentReference(),
                PaymentEventOutcome.valueOf(
                        requireText(result.outcome(), "Payment event outcome")
                                .toUpperCase(Locale.ROOT)
                ),
                Objects.requireNonNull(result.checks(), "Payment execution checks")
                        .stream()
                        .map(check ->
                                new PaymentEventExecutionCheck(
                                        FundsControlCheckType.valueOf(
                                                requireText(
                                                        check.type(),
                                                        "Execution check type"
                                                )
                                        ),
                                        EvidenceCheckResult.valueOf(
                                                requireText(
                                                        check.result(),
                                                        "Execution check result"
                                                )
                                        ),
                                        check.reasonCode() == null
                                                ? null
                                                : FailureCode.of(check.reasonCode())
                                )
                        )
                        .toList(),
                result.bankReference(),
                result.reasonCode() == null
                        ? null
                        : FailureCode.of(result.reasonCode()),
                result.observedAt()
        );
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.strip();
    }
}
