package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import com.sixpay.payment.domain.model.evidence.PaymentEventOutcome;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public interface PaymentEventExecutionPort {

    PaymentEventExecutionResult execute(PaymentEventExecutionCommand command);

    record PaymentEventExecutionCommand(
            PaymentFinancialEventSnapshot snapshot,
            PaymentEventMappingContext mappingContext,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey
    ) {
        public PaymentEventExecutionCommand {
            snapshot = Objects.requireNonNull(snapshot, "Finalized Payment financial snapshot");
            mappingContext = Objects.requireNonNull(mappingContext, "Payment event mapping context");
            context = Objects.requireNonNull(context, "Banking request context");
            idempotencyKey = Objects.requireNonNull(idempotencyKey, "Banking idempotency key");
        }
    }

    record PaymentEventMappingContext(
            String operationCode,
            long eventNumber,
            LocalDate accountingDate,
            boolean nightMode,
            String technicalUser,
            Instant requestedAt
    ) {
        public PaymentEventMappingContext {
            if (operationCode == null || operationCode.isBlank()) {
                throw new IllegalArgumentException("Operation code must not be blank");
            }
            if (eventNumber <= 0) {
                throw new IllegalArgumentException("Event number must be positive");
            }
            accountingDate = Objects.requireNonNull(accountingDate, "Accounting date");
            if (technicalUser == null || technicalUser.isBlank()) {
                throw new IllegalArgumentException("Technical user must not be blank");
            }
            requestedAt = Objects.requireNonNull(requestedAt, "Requested at");
            operationCode = operationCode.strip();
            technicalUser = technicalUser.strip();
        }
    }

    record PaymentEventExecutionCheck(
            FundsControlCheckType type,
            EvidenceCheckResult result,
            FailureCode reasonCode
    ) {
        public PaymentEventExecutionCheck {
            type = Objects.requireNonNull(type, "Execution check type");
            result = Objects.requireNonNull(result, "Execution check result");
        }
    }

    record PaymentEventExecutionResult(
            String paymentReference,
            PaymentEventOutcome outcome,
            List<PaymentEventExecutionCheck> checks,
            String bankReference,
            FailureCode reasonCode,
            Instant observedAt
    ) {
        public PaymentEventExecutionResult {
            if (paymentReference == null || paymentReference.isBlank()) {
                throw new IllegalArgumentException("Payment reference must not be blank");
            }
            paymentReference = paymentReference.strip();
            outcome = Objects.requireNonNull(outcome, "Payment event outcome");
            checks = List.copyOf(Objects.requireNonNull(checks, "Payment execution checks"));
            observedAt = Objects.requireNonNull(observedAt, "Observed at");
        }
    }

    final class PaymentEventOutcomeUnknownException
            extends RuntimeException {

        public PaymentEventOutcomeUnknownException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }

        public PaymentEventOutcomeUnknownException(
                String message
        ) {
            super(message);
        }
    }
}
