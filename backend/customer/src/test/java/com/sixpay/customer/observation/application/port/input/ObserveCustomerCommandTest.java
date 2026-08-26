package com.sixpay.customer.observation.application.port.input;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObserveCustomerCommandTest {

    private static final UUID SOURCE_EVENT_ID =
            UUID.fromString(
                    "54e671e0-5a2a-4af7-bf70-90dfdd555837"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
            );

    private static final String CORRELATION_ID =
            "c74e165f-df46-463e-a520-188e6df3e5ae";

    @Test
    void normalizesAndCreatesCustomerOwnedDomainValues() {
        ObserveCustomerCommand command = validCommand();

        assertEquals(
                "M0123456",
                command.normalizedNiu()
        );
        assertEquals(
                "Société ABC SARL",
                command.legalName()
        );
        assertEquals(
                "AMPLITUDE",
                command.financialInstitutionCode()
        );
        assertEquals(
                "XAF",
                command.currency()
        );

        assertEquals(
                command.normalizedNiu(),
                command.identity().normalizedNiu()
        );
        assertEquals(
                command.accountBindingFingerprint(),
                command.accountReference()
                        .accountBindingFingerprint()
        );
        assertEquals(
                SOURCE_EVENT_ID.toString(),
                command.watermark().value()
        );
        assertEquals(
                PAYMENT_ID,
                command.payment().paymentId()
        );
    }

    @Test
    void rejectsUnmaskedContactsAndRawAccountReference() {
        ObserveCustomerCommand valid = validCommand();

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> copyWith(
                        valid,
                        "6135551234",
                        valid.email(),
                        valid.maskedAccountReference()
                )
        );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> copyWith(
                        valid,
                        valid.phone(),
                        valid.email(),
                        "10005-00001-12345678901-12"
                )
        );
    }

    @Test
    void rejectsInvalidDatesAndCorrelationId() {
        ObserveCustomerCommand valid = validCommand();

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> new ObserveCustomerCommand(
                        valid.sourceEventId(),
                        valid.paymentId(),
                        valid.paymentReference(),
                        valid.normalizedNiu(),
                        valid.legalName(),
                        valid.phone(),
                        valid.email(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.maskedAccountReference(),
                        valid.amount(),
                        valid.currency(),
                        valid.paymentStatus(),
                        valid.failureReasonCode(),
                        valid.paymentUpdatedAt(),
                        valid.paymentCreatedAt(),
                        valid.observedAt(),
                        valid.correlationId()
                )
        );

        assertThrows(
                ObservedCustomerDomainException.class,
                () -> new ObserveCustomerCommand(
                        valid.sourceEventId(),
                        valid.paymentId(),
                        valid.paymentReference(),
                        valid.normalizedNiu(),
                        valid.legalName(),
                        valid.phone(),
                        valid.email(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.maskedAccountReference(),
                        valid.amount(),
                        valid.currency(),
                        valid.paymentStatus(),
                        valid.failureReasonCode(),
                        valid.paymentCreatedAt(),
                        valid.paymentUpdatedAt(),
                        valid.observedAt(),
                        "corr-observation"
                )
        );
    }

    @Test
    void toStringRedactsAllProtectedFields() {
        ObserveCustomerCommand command = validCommand();
        String rendered = command.toString();

        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Société ABC SARL"));
        assertFalse(rendered.contains("***-***-1234"));
        assertFalse(rendered.contains("a***@example.com"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
        assertFalse(rendered.contains("•••• 1234"));
    }

    private static ObserveCustomerCommand validCommand() {
        return new ObserveCustomerCommand(
                SOURCE_EVENT_ID,
                PAYMENT_ID,
                "PAY-2026-000123",
                " m 0123456 ",
                "  Société   ABC SARL ",
                "***-***-1234",
                "a***@example.com",
                "amplitude",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("15000.00"),
                "xaf",
                ObservedPaymentStatus.BANKING_CHECKING,
                null,
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse("2026-08-03T20:05:00Z"),
                Instant.parse("2026-08-03T20:05:01Z"),
                CORRELATION_ID
        );
    }

    private static ObserveCustomerCommand copyWith(
            ObserveCustomerCommand value,
            String phone,
            String email,
            String maskedAccountReference
    ) {
        return new ObserveCustomerCommand(
                value.sourceEventId(),
                value.paymentId(),
                value.paymentReference(),
                value.normalizedNiu(),
                value.legalName(),
                phone,
                email,
                value.financialInstitutionCode(),
                value.accountBindingFingerprint(),
                maskedAccountReference,
                value.amount(),
                value.currency(),
                value.paymentStatus(),
                value.failureReasonCode(),
                value.paymentCreatedAt(),
                value.paymentUpdatedAt(),
                value.observedAt(),
                value.correlationId()
        );
    }
}
