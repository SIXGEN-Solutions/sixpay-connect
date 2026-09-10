package com.sixpay.payment.infrastructure.banking.amplitude.posting;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort.PaymentEventRecoveryQuery;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort.PaymentEventRecoveryStatus;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.evidence.PaymentEventObservationSource;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.AmplitudePaymentEventRecoveryClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.dto.AmplitudePaymentEventResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmplitudePaymentEventRecoveryAdapterTest {

    @Test
    void returnsCompletedFromPaymentReferenceWithoutSecondLookup() {
        StubClient client = new StubClient(
                Optional.of(result("COMPLETED")),
                Optional.empty()
        );
        AmplitudePaymentEventRecoveryAdapter adapter =
                new AmplitudePaymentEventRecoveryAdapter(client);

        var recovery = adapter.recover(query());
        assertEquals(
                PaymentEventRecoveryStatus.COMPLETED,
                recovery.status()
        );
        assertEquals(
                PaymentEventObservationSource.PAYMENT_REFERENCE_LOOKUP,
                recovery.source()
        );
        assertEquals(1, client.paymentReferenceCalls);
        assertEquals(0, client.idempotencyCalls);
    }

    @Test
    void fallsBackToIdempotencyLookupWhenReferenceNotFound() {
        StubClient client = new StubClient(
                Optional.empty(),
                Optional.of(result("REJECTED"))
        );
        AmplitudePaymentEventRecoveryAdapter adapter =
                new AmplitudePaymentEventRecoveryAdapter(client);

        var recovery = adapter.recover(query());
        assertEquals(
                PaymentEventRecoveryStatus.REJECTED,
                recovery.status()
        );
        assertEquals(
                PaymentEventObservationSource.IDEMPOTENCY_LOOKUP,
                recovery.source()
        );
        assertEquals(1, client.paymentReferenceCalls);
        assertEquals(1, client.idempotencyCalls);
    }

    @Test
    void returnsNotFoundWhenBothLookupsMiss() {
        StubClient client = new StubClient(
                Optional.empty(),
                Optional.empty()
        );
        AmplitudePaymentEventRecoveryAdapter adapter =
                new AmplitudePaymentEventRecoveryAdapter(client);

        assertEquals(
                PaymentEventRecoveryStatus.NOT_FOUND,
                adapter.recover(query()).status()
        );
    }

    @Test
    void returnsUnknownWhenProviderResultIsIndeterminate() {
        StubClient client = new StubClient(
                Optional.of(result("UNKNOWN")),
                Optional.empty()
        );
        AmplitudePaymentEventRecoveryAdapter adapter =
                new AmplitudePaymentEventRecoveryAdapter(client);

        assertEquals(
                PaymentEventRecoveryStatus.UNKNOWN,
                adapter.recover(query()).status()
        );
        assertEquals(1, client.idempotencyCalls);
    }

    private static PaymentEventRecoveryQuery query() {
        return new PaymentEventRecoveryQuery(
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                new BankingRequestContext(
                        CorrelationId.of(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        FinancialInstitutionCode.of("LRB")
                ),
                new BankingIdempotencyKey(
                        "idem-1234567890123456"
                )
        );
    }

    private static AmplitudePaymentEventResult result(
            String outcome
    ) {
        return new AmplitudePaymentEventResult(
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                outcome,
                List.of(),
                "BK-1",
                null,
                Instant.parse("2026-09-06T18:00:05Z")
        );
    }

    private static final class StubClient
            implements AmplitudePaymentEventRecoveryClient {

        private final Optional<AmplitudePaymentEventResult> byReference;
        private final Optional<AmplitudePaymentEventResult> byIdempotency;
        private int paymentReferenceCalls;
        private int idempotencyCalls;

        private StubClient(
                Optional<AmplitudePaymentEventResult> byReference,
                Optional<AmplitudePaymentEventResult> byIdempotency
        ) {
            this.byReference = byReference;
            this.byIdempotency = byIdempotency;
        }

        @Override
        public Optional<AmplitudePaymentEventResult> findByPaymentReference(
                String paymentReference,
                String correlationId,
                String financialInstitutionCode
        ) {
            paymentReferenceCalls++;
            return byReference;
        }

        @Override
        public Optional<AmplitudePaymentEventResult> findByIdempotencyKey(
                String idempotencyKey,
                String correlationId,
                String financialInstitutionCode
        ) {
            idempotencyCalls++;
            return byIdempotency;
        }
    }
}
