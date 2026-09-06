package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventRecoveryClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class AmplitudePaymentEventRecoveryAdapter
        implements PaymentEventRecoveryPort {

    private final AmplitudePaymentEventRecoveryClient client;

    public AmplitudePaymentEventRecoveryAdapter(
            AmplitudePaymentEventRecoveryClient client
    ) {
        this.client = Objects.requireNonNull(
                client,
                "Amplitude Payment event recovery client"
        );
    }

    @Override
    public PaymentEventRecoveryResult recover(
            PaymentEventRecoveryQuery query
    ) {
        Objects.requireNonNull(
                query,
                "Payment event recovery query"
        );

        String correlationId =
                query.context().correlationId().value();
        String financialInstitutionCode =
                query.context()
                        .financialInstitutionCode()
                        .value();

        Optional<AmplitudePaymentEventResult> byReference =
                client.findByPaymentReference(
                        query.paymentReference().value(),
                        correlationId,
                        financialInstitutionCode
                );

        if (byReference.isPresent()) {
            PaymentEventRecoveryResult result =
                    classify(byReference.get());
            if (result.status()
                    != PaymentEventRecoveryStatus.UNKNOWN) {
                return result;
            }
        }

        Optional<AmplitudePaymentEventResult> byIdempotency =
                client.findByIdempotencyKey(
                        query.idempotencyKey().value(),
                        correlationId,
                        financialInstitutionCode
                );

        if (byIdempotency.isPresent()) {
            return classify(byIdempotency.get());
        }

        return byReference
                .map(this::classify)
                .orElseGet(
                        PaymentEventRecoveryResult::notFound
                );
    }

    private PaymentEventRecoveryResult classify(
            AmplitudePaymentEventResult result
    ) {
        Objects.requireNonNull(
                result,
                "Amplitude Payment event result"
        );

        String outcome = result.outcome();
        if (outcome == null || outcome.isBlank()) {
            return new PaymentEventRecoveryResult(
                    PaymentEventRecoveryStatus.UNKNOWN,
                    result
            );
        }

        return switch (
                outcome.strip()
                        .toUpperCase(Locale.ROOT)
        ) {
            case "COMPLETED" ->
                    new PaymentEventRecoveryResult(
                            PaymentEventRecoveryStatus.COMPLETED,
                            result
                    );
            case "REJECTED" ->
                    new PaymentEventRecoveryResult(
                            PaymentEventRecoveryStatus.REJECTED,
                            result
                    );
            default ->
                    new PaymentEventRecoveryResult(
                            PaymentEventRecoveryStatus.UNKNOWN,
                            result
                    );
        };
    }
}
