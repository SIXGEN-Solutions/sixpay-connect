package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.evidence.PaymentEventObservationSource;

import java.util.Objects;

public interface PaymentEventRecoveryPort {

    PaymentEventRecoveryResult recover(PaymentEventRecoveryQuery query);

    record PaymentEventRecoveryQuery(
            PublicPaymentReference paymentReference,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    ) {
        public PaymentEventRecoveryQuery {
            paymentReference = Objects.requireNonNull(paymentReference, "Payment reference");
            context = Objects.requireNonNull(context, "Banking request context");
            idempotencyKey = Objects.requireNonNull(idempotencyKey, "Banking idempotency key");
        }
    }

    enum PaymentEventRecoveryStatus {
        COMPLETED,
        REJECTED,
        UNKNOWN,
        NOT_FOUND
    }

    record PaymentEventRecoveryResult(
            PaymentEventRecoveryStatus status,
            PaymentEventExecutionPort.PaymentEventExecutionResult result,
            PaymentEventObservationSource source
    ) {
        public PaymentEventRecoveryResult {
            status = Objects.requireNonNull(status, "Recovery status");

            if (status == PaymentEventRecoveryStatus.NOT_FOUND) {
                if (result != null || source != null) {
                    throw new IllegalArgumentException(
                            "NOT_FOUND recovery result must not carry a result or source"
                    );
                }
            } else {
                result = Objects.requireNonNull(result, "Recovery result");
                source = Objects.requireNonNull(source, "Recovery source");
                if (source == PaymentEventObservationSource.DIRECT_RESPONSE) {
                    throw new IllegalArgumentException(
                            "Recovery source must be a lookup source"
                    );
                }
            }
        }

        public static PaymentEventRecoveryResult notFound() {
            return new PaymentEventRecoveryResult(
                    PaymentEventRecoveryStatus.NOT_FOUND,
                    null,
                    null
            );
        }
    }
}
