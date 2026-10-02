package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.payment.api.response.TresorPayPaymentRecoveryResponse;
import com.sixpay.payment.application.view.TresorPayPaymentRecoveryView;
import com.sixpay.payment.application.port.input.TresorPayPaymentRecoveryUseCase;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TresorPayPaymentRecoveryControllerTest {

    @Test
    void returnsRecoveryViewAndEchoesCorrelationId() {
        TresorPayPaymentRecoveryUseCase useCase =
                mock(TresorPayPaymentRecoveryUseCase.class);
        CorrelationIdResolver resolver = mock(CorrelationIdResolver.class);

        String reference = "PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X";
        String correlation = "11111111-1111-1111-1111-111111111111";

        when(resolver.resolve(correlation))
                .thenReturn(CorrelationId.of(correlation));

        var view = new TresorPayPaymentRecoveryView(
                UUID.randomUUID(),
                reference,
                "AVI-DEMO-00045678",
                "PENDING_CONFIRMATION",
                new TresorPayPaymentRecoveryView.Money(
                        new BigDecimal("600000"),
                        "XAF"
                ),
                Instant.parse("2026-08-03T10:30:01Z"),
                Instant.parse("2026-08-03T10:30:02Z"),
                null
        );

        when(useCase.findByPaymentReference(
                PublicPaymentReference.of(reference)
        )).thenReturn(Optional.of(view));

        var controller = new TresorPayPaymentRecoveryController(
                useCase,
                resolver
        );

        var response = controller.getPayment(reference, correlation);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().paymentReference())
                .isEqualTo(view.paymentReference());
        assertThat(response.getBody().tresorPayPaymentReference())
                .isEqualTo(view.tresorPayPaymentReference());
        assertThat(response.getBody().status())
                .isEqualTo(view.status());
        assertThat(response.getHeaders().getFirst("X-Correlation-ID"))
                .isEqualTo(correlation);
    }

    @Test
    void throwsPaymentNotFoundForUnknownReference() {
        TresorPayPaymentRecoveryUseCase useCase =
                mock(TresorPayPaymentRecoveryUseCase.class);
        CorrelationIdResolver resolver = mock(CorrelationIdResolver.class);

        String reference = "PAY-0H7Y5A2C9M6K4N8Q1R3T5V7W9X";
        String correlation = "11111111-1111-1111-1111-111111111111";

        when(resolver.resolve(correlation))
                .thenReturn(CorrelationId.of(correlation));
        when(useCase.findByPaymentReference(
                PublicPaymentReference.of(reference)
        )).thenReturn(Optional.empty());

        var controller = new TresorPayPaymentRecoveryController(
                useCase,
                resolver
        );

        assertThatThrownBy(() ->
                controller.getPayment(reference, correlation)
        )
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(reference);
    }

    @Test
    void correlationHeaderIsDeclaredRequired() throws Exception {
        var parameter = TresorPayPaymentRecoveryController.class
                .getMethod("getPayment", String.class, String.class)
                .getParameters()[1];

        var annotation = parameter.getAnnotation(
                org.springframework.web.bind.annotation.RequestHeader.class
        );

        assertThat(annotation).isNotNull();
        assertThat(annotation.required()).isTrue();
    }

}
