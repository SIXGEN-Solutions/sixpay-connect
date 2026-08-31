package com.sixpay.payment.application.service;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentPostPersistenceOrchestrationServiceTest {

    @Mock
    private PaymentMutationCoordinator coordinator;

    @Mock
    private ObjectProvider<PaymentCustomerVerificationService>
            customerVerificationServiceProvider;

    @Mock
    private PaymentCustomerVerificationService customerVerificationService;

    @Mock
    private PaymentPolicyBundle policies;

    @Mock
    private TimeProvider timeProvider;

    @Test
    void relayedPaymentReceivedStartsNextDurableMutation() {
        UUID aggregateId = UUID.randomUUID();
        Instant dispatchedAt = Instant.parse("2026-08-31T16:20:01Z");
        when(timeProvider.now()).thenReturn(dispatchedAt);

        service().handle(event("PaymentReceived", "PAYMENT", aggregateId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Payment>> mutation =
                ArgumentCaptor.forClass(Consumer.class);

        verify(coordinator).mutate(
                eq(new PaymentId(aggregateId)),
                mutation.capture()
        );

        assertThat(mutation.getValue()).isNotNull();
        verifyNoInteractions(customerVerificationService);
    }

    @Test
    void relayedBankingRequestInvokesCustomerVerification() {
        UUID aggregateId = UUID.randomUUID();
        Instant dispatchedAt = Instant.parse("2026-08-31T16:20:02Z");

        when(timeProvider.now()).thenReturn(dispatchedAt);
        when(customerVerificationServiceProvider.getIfAvailable())
                .thenReturn(customerVerificationService);

        service().handle(event(
                "PaymentBankingVerificationRequested",
                "PAYMENT",
                aggregateId
        ));

        verify(customerVerificationService).verifyCustomer(
                new PaymentId(aggregateId),
                dispatchedAt,
                policies
        );
        verifyNoInteractions(coordinator);
    }

    @Test
    void unavailableCustomerBridgeFailsEventForOutboxRetry() {
        UUID aggregateId = UUID.randomUUID();
        when(customerVerificationServiceProvider.getIfAvailable())
                .thenReturn(null);

        assertThatThrownBy(() -> service().handle(event(
                "PaymentBankingVerificationRequested",
                "PAYMENT",
                aggregateId
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bridge is unavailable");

        verifyNoInteractions(coordinator, customerVerificationService);
    }

    @Test
    void ignoresUnrelatedEvents() {
        service().handle(event(
                "PaymentCustomerConfirmationRequested",
                "PAYMENT",
                UUID.randomUUID()
        ));
        service().handle(event(
                "PaymentReceived",
                "CUSTOMER",
                UUID.randomUUID()
        ));

        verifyNoInteractions(
                coordinator,
                customerVerificationServiceProvider,
                customerVerificationService,
                timeProvider
        );
    }

    private PaymentPostPersistenceOrchestrationService service() {
        return new PaymentPostPersistenceOrchestrationService(
                coordinator,
                customerVerificationServiceProvider,
                policies,
                timeProvider
        );
    }

    private static IntegrationEventEnvelope event(
            String eventType,
            String aggregateType,
            UUID aggregateId
    ) {
        return new IntegrationEventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                aggregateType,
                aggregateId,
                UUID.randomUUID().toString(),
                Instant.parse("2026-08-31T16:20:00Z"),
                "{\"event\":\"test\"}"
        );
    }
}
