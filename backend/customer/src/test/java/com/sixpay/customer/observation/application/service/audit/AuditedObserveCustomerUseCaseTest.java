package com.sixpay.customer.observation.application.service.audit;

import com.sixpay.customer.observation.application.audit.*;
import com.sixpay.customer.observation.application.port.input.*;
import com.sixpay.customer.observation.application.port.output.audit.*;
import com.sixpay.customer.observation.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditedObserveCustomerUseCaseTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T16:00:00Z");

    @Test
    void appliedProjectionIsAuditedAndReturned() {
        ObserveCustomerCommand command = command(
                ObservedPaymentStatus.RECEIVED,
                null
        );
        ObserveCustomerResult expected = new ObserveCustomerResult(
                ObservedCustomerId.of(
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                ),
                command.sourceEventId(),
                command.paymentId(),
                ObserveCustomerResult.Disposition.APPLIED,
                1,
                NOW
        );

        ObserveCustomerUseCase delegate =
                mock(ObserveCustomerUseCase.class);
        when(delegate.observe(command)).thenReturn(expected);

        AtomicReference<ObservedCustomerAuditRecord> captured =
                new AtomicReference<>();

        AuditedObserveCustomerUseCase service =
                new AuditedObserveCustomerUseCase(
                        delegate,
                        captured::set,
                        () -> UUID.fromString(
                                "99999999-9999-4999-8999-999999999999"
                        ),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertSame(expected, service.observe(command));
        assertEquals(
                ObservedCustomerAuditAction.PROJECTION_APPLIED,
                captured.get().action()
        );
        assertEquals(
                ObservedCustomerAuditOutcome.SUCCEEDED,
                captured.get().outcome()
        );
    }

    @Test
    void auditFailurePreventsSuccessfulReturn() {
        ObserveCustomerCommand command = command(
                ObservedPaymentStatus.RECEIVED,
                null
        );
        ObserveCustomerUseCase delegate =
                ignored -> new ObserveCustomerResult(
                        ObservedCustomerId.of(
                                UUID.fromString(
                                        "44444444-4444-4444-8444-444444444444"
                                )
                        ),
                        command.sourceEventId(),
                        command.paymentId(),
                        ObserveCustomerResult.Disposition.APPLIED,
                        1,
                        NOW
                );

        AuditedObserveCustomerUseCase service =
                new AuditedObserveCustomerUseCase(
                        delegate,
                        record -> {
                            throw new IllegalStateException(
                                    "audit unavailable"
                            );
                        },
                        UUID::randomUUID,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.observe(command)
        );
    }

    private static ObserveCustomerCommand command(
            ObservedPaymentStatus status,
            String failure
    ) {
        return new ObserveCustomerCommand(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                "PAY-001",
                "M0123456",
                "Société ABC",
                "***-***-1234",
                "a***@example.com",
                "BANK",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("100.00"),
                "XAF",
                status,
                failure,
                NOW.minusSeconds(10),
                NOW,
                NOW,
                "55555555-5555-4555-8555-555555555555"
        );
    }
}
