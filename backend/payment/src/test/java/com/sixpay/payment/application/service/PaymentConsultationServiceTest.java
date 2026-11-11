package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentConsultationServiceTest {

    @Test
    void mapsAuthoritativePaymentStateToRecoveryView() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        Payment payment = PaymentAggregateTestFixtures.newPayment();
        PublicPaymentReference reference = payment.publicPaymentReference();

        when(lookup.findByPublicPaymentReference(reference))
                .thenReturn(Optional.of(payment));

        var guard = mock(PaymentConfirmationPartnerAccessGuard.class);
        when(guard.isAllowed(reference.value())).thenReturn(true);
        var service = new PaymentConsultationService(lookup, guard);

        var result = service.findByPaymentReference(reference);

        assertThat(result).isPresent();

        var view = result.orElseThrow();
        var state = payment.toState();

        assertThat(view.paymentId())
                .isEqualTo(state.paymentId().value());
        assertThat(view.paymentReference())
                .isEqualTo(state.publicPaymentReference().value());
        assertThat(view.externalPaymentReference())
                .isEqualTo(state.externalPaymentReference().value());
        assertThat(view.status())
                .isEqualTo(state.status().name());
        assertThat(view.amount().amount())
                .isEqualByComparingTo(state.requestedAmount().amount());
        assertThat(view.amount().currency())
                .isEqualTo(state.requestedAmount().currency().getCurrencyCode());
        assertThat(view.receivedAt())
                .isEqualTo(state.receivedAt());
        assertThat(view.updatedAt())
                .isEqualTo(state.updatedAt());
        assertThat(view.finalizedAt())
                .isEqualTo(state.finalizedAt().orElse(null));

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

        var guard = mock(PaymentConfirmationPartnerAccessGuard.class);
        when(guard.isAllowed(reference.value())).thenReturn(true);
        var service = new PaymentConsultationService(lookup, guard);

        assertThat(service.findByPaymentReference(reference)).isEmpty();

        verify(lookup).findByPublicPaymentReference(reference);
    }

    @Test
    void hidesPaymentWhenCallerDoesNotOwnIt() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        PaymentConfirmationPartnerAccessGuard guard =
                mock(PaymentConfirmationPartnerAccessGuard.class);
        PublicPaymentReference reference = PublicPaymentReference.of(
                "PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X");
        when(guard.isAllowed(reference.value())).thenReturn(false);

        var result = new PaymentConsultationService(lookup, guard)
                .findByPaymentReference(reference);

        assertThat(result).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(lookup);
    }

}
