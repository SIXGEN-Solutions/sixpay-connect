package com.sixpay.customer.observation.application.service;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.output.ObservedCustomerIdGenerator;
import com.sixpay.customer.observation.application.port.output.ObservedCustomerRepository;
import com.sixpay.customer.observation.application.port.output.ObservedPaymentRepository;
import com.sixpay.customer.observation.domain.model.ObservedCustomer;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentReference;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import com.sixpay.customer.observation.domain.model.ProjectionWatermark;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ObservedCustomerProjectionServiceTest {

    private static final ObservedCustomerId CUSTOMER_ID =
            ObservedCustomerId.of(
                    UUID.fromString(
                            "901a3933-ae9e-4eb3-9fcf-f368a350a1db"
                    )
            );

    @Test
    void firstObservationCreatesAndPersistsProjectionAndPayment() {
        InMemoryCustomerRepository customers =
                new InMemoryCustomerRepository();
        CapturingPaymentRepository payments =
                new CapturingPaymentRepository();

        var service = service(
                customers,
                payments
        );

        ObserveCustomerCommand command = command(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                ObservedPaymentStatus.RECEIVED,
                null,
                "2026-08-03T20:00:00Z",
                "2026-08-03T20:00:01Z"
        );

        ObserveCustomerResult result =
                service.observe(command);

        assertEquals(
                ObserveCustomerResult.Disposition.APPLIED,
                result.disposition()
        );
        assertEquals(CUSTOMER_ID, result.observedCustomerId());
        assertEquals(1, result.projectionVersion());
        assertEquals(1, customers.saveCalls);
        assertEquals(1, payments.saveCalls);
        assertEquals(
                command.sourceEventId(),
                payments.sourceEventId
        );
        assertEquals(
                command.paymentId(),
                payments.payment.paymentId()
        );
    }

    @Test
    void subsequentObservationUpdatesExistingProjection() {
        InMemoryCustomerRepository customers =
                new InMemoryCustomerRepository();
        CapturingPaymentRepository payments =
                new CapturingPaymentRepository();
        var service = service(customers, payments);

        service.observe(command(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                ObservedPaymentStatus.RECEIVED,
                null,
                "2026-08-03T20:00:00Z",
                "2026-08-03T20:00:01Z"
        ));

        ObserveCustomerResult result = service.observe(command(
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                UUID.fromString(
                        "54e671e0-5a2a-4af7-bf70-90dfdd555837"
                ),
                ObservedPaymentStatus.REJECTED,
                "ACCOUNT_NOT_FOUND",
                "2026-08-03T20:05:00Z",
                "2026-08-03T20:05:01Z"
        ));

        assertEquals(
                ObserveCustomerResult.Disposition.APPLIED,
                result.disposition()
        );
        assertEquals(2, result.projectionVersion());
        assertEquals(2, customers.customer.totalPayments());
        assertEquals(1, customers.customer.failedPayments());
        assertEquals(2, customers.saveCalls);
        assertEquals(2, payments.saveCalls);
    }

    @Test
    void replayDoesNotPersistProjectionOrPaymentAgain() {
        InMemoryCustomerRepository customers =
                new InMemoryCustomerRepository();
        CapturingPaymentRepository payments =
                new CapturingPaymentRepository();
        var service = service(customers, payments);

        ObserveCustomerCommand command = command(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                ObservedPaymentStatus.RECEIVED,
                null,
                "2026-08-03T20:00:00Z",
                "2026-08-03T20:00:01Z"
        );

        service.observe(command);

        int customerSaves = customers.saveCalls;
        int paymentSaves = payments.saveCalls;

        ObserveCustomerResult replay =
                service.observe(command);

        assertEquals(
                ObserveCustomerResult.Disposition.REPLAYED,
                replay.disposition()
        );
        assertEquals(customerSaves, customers.saveCalls);
        assertEquals(paymentSaves, payments.saveCalls);
    }

    @Test
    void staleObservationIsPersistedButReportedAsIgnoredStale() {
        InMemoryCustomerRepository customers =
                new InMemoryCustomerRepository();
        CapturingPaymentRepository payments =
                new CapturingPaymentRepository();
        var service = service(customers, payments);

        UUID paymentId = UUID.fromString(
                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
        );

        service.observe(command(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                paymentId,
                ObservedPaymentStatus.RECEIVED,
                null,
                "2026-08-03T20:00:00Z",
                "2026-08-03T20:00:01Z"
        ));

        service.observe(command(
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                paymentId,
                ObservedPaymentStatus.DEBITED,
                null,
                "2026-08-03T20:10:00Z",
                "2026-08-03T20:10:01Z"
        ));

        ObserveCustomerResult stale = service.observe(command(
                UUID.fromString(
                        "33333333-3333-4333-8333-333333333333"
                ),
                paymentId,
                ObservedPaymentStatus.REJECTED,
                "ACCOUNT_NOT_FOUND",
                "2026-08-03T20:04:00Z",
                "2026-08-03T20:11:00Z"
        ));

        assertEquals(
                ObserveCustomerResult.Disposition.IGNORED_STALE,
                stale.disposition()
        );
        assertEquals(
                ObservedPaymentStatus.DEBITED,
                customers.customer.lastPaymentStatus()
        );
        assertEquals(3, payments.saveCalls);
    }

    @Test
    void idGeneratorIsUsedOnlyForFirstProjection() {
        InMemoryCustomerRepository customers =
                new InMemoryCustomerRepository();
        CapturingPaymentRepository payments =
                new CapturingPaymentRepository();
        CountingIdGenerator ids = new CountingIdGenerator();

        var service = new ObservedCustomerProjectionService(
                customers,
                payments,
                ids
        );

        service.observe(command(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                ObservedPaymentStatus.RECEIVED,
                null,
                "2026-08-03T20:00:00Z",
                "2026-08-03T20:00:01Z"
        ));

        service.observe(command(
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                UUID.fromString(
                        "54e671e0-5a2a-4af7-bf70-90dfdd555837"
                ),
                ObservedPaymentStatus.RECEIVED,
                null,
                "2026-08-03T20:05:00Z",
                "2026-08-03T20:05:01Z"
        ));

        assertEquals(1, ids.calls);
        assertSame(customers.customer, customers.savedReference);
    }

    private static ObservedCustomerProjectionService service(
            InMemoryCustomerRepository customers,
            CapturingPaymentRepository payments
    ) {
        return new ObservedCustomerProjectionService(
                customers,
                payments,
                () -> CUSTOMER_ID
        );
    }

    private static ObserveCustomerCommand command(
            UUID sourceEventId,
            UUID paymentId,
            ObservedPaymentStatus status,
            String failureCode,
            String paymentUpdatedAt,
            String observedAt
    ) {
        return new ObserveCustomerCommand(
                sourceEventId,
                paymentId,
                "PAY-" + paymentId,
                "M0123456",
                "Société ABC SARL",
                "***-***-1234",
                "a***@example.com",
                "AMPLITUDE",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("15000.00"),
                "XAF",
                status,
                failureCode,
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse(paymentUpdatedAt),
                Instant.parse(observedAt),
                "c74e165f-df46-463e-a520-188e6df3e5ae"
        );
    }

    private static final class InMemoryCustomerRepository
            implements ObservedCustomerRepository {

        private ObservedCustomer customer;
        private ObservedCustomer savedReference;
        private int saveCalls;

        @Override
        public Optional<ObservedCustomer> findByNormalizedNiu(
                String normalizedNiu
        ) {
            if (customer == null) {
                return Optional.empty();
            }

            return customer.identity()
                    .normalizedNiu()
                    .equals(normalizedNiu)
                    ? Optional.of(customer)
                    : Optional.empty();
        }

        @Override
        public void save(ObservedCustomer observedCustomer) {
            this.customer = observedCustomer;
            this.savedReference = observedCustomer;
            this.saveCalls++;
        }
    }

    private static final class CapturingPaymentRepository
            implements ObservedPaymentRepository {

        private ObservedCustomerId customerId;
        private UUID sourceEventId;
        private ObservedPaymentReference payment;
        private ProjectionWatermark watermark;
        private Instant observedAt;
        private int saveCalls;

        @Override
        public void save(
                ObservedCustomerId observedCustomerId,
                UUID sourceEventId,
                ObservedPaymentReference payment,
                ProjectionWatermark watermark,
                Instant observedAt
        ) {
            this.customerId = observedCustomerId;
            this.sourceEventId = sourceEventId;
            this.payment = payment;
            this.watermark = watermark;
            this.observedAt = observedAt;
            this.saveCalls++;
        }
    }

    private static final class CountingIdGenerator
            implements ObservedCustomerIdGenerator {

        private int calls;

        @Override
        public ObservedCustomerId nextId() {
            calls++;
            return CUSTOMER_ID;
        }
    }
}
