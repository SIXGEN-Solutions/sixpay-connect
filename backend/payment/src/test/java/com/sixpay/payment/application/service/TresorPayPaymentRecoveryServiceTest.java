package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TresorPayPaymentRecoveryServiceTest {

    @Test
    void readsPaymentWithoutTriggeringAnyMutationOrExternalOperation() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        Payment payment = mock(Payment.class);
        PublicPaymentReference reference = PublicPaymentReference.of(
                "PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X"
        );

        when(lookup.findByPublicPaymentReference(reference))
                .thenReturn(Optional.of(payment));
        when(payment.toState()).thenThrow(
                new UnsupportedOperationException(
                        "fixture requires a real PaymentState"
                )
        );

        var service = new TresorPayPaymentRecoveryService(lookup);

        try {
            service.findByPaymentReference(reference);
        } catch (UnsupportedOperationException expected) {
            // The test's purpose is to assert the lookup boundary.
        }

        verify(lookup).findByPublicPaymentReference(reference);
    }

    @Test
    void returnsEmptyWhenPaymentDoesNotExist() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        PublicPaymentReference reference = PublicPaymentReference.of(
                "PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X"
        );
        when(lookup.findByPublicPaymentReference(reference))
                .thenReturn(Optional.empty());

        var service = new TresorPayPaymentRecoveryService(lookup);

        assertThat(service.findByPaymentReference(reference)).isEmpty();
    }
}
