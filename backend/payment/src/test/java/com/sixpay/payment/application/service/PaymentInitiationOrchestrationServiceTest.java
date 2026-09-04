package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.InitiateDebitBeneficiaryCommand;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.output.idempotency.PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.output.initiation.PaymentInitiationPreparationPort;
import com.sixpay.payment.application.port.output.initiation.PreparedPaymentInitiation;
import com.sixpay.payment.application.view.InitiateDebitResult;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaymentInitiationOrchestrationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-03T10:30:00Z");

    @Test
    void persistsOneNewPaymentInReceived() {
        PaymentInitiationIdempotencyPort idempotencyPort =
                Mockito.mock(
                        PaymentInitiationIdempotencyPort.class
                );
        PaymentInitiationPreparationPort preparationPort =
                Mockito.mock(
                        PaymentInitiationPreparationPort.class
                );
        PaymentReceptionService receptionService =
                Mockito.mock(PaymentReceptionService.class);
        TimeProvider timeProvider = () -> NOW;

        InitiateDebitCommand command = command();
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());
        PublicPaymentReference reference =
                PublicPaymentReference.of(
                        "PAY-1234567890ABCDEFGHJKMNPQRS"
                );
        NewPaymentIntent intent =
                Mockito.mock(NewPaymentIntent.class);

        when(idempotencyPort.execute(
                any(),
                any()
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<String, InitiateDebitResult> action =
                    invocation.getArgument(1);

            return action.apply("a".repeat(64));
        });

        when(preparationPort.prepare(
                command,
                "a".repeat(64),
                NOW
        )).thenReturn(
                new PreparedPaymentInitiation(
                        paymentId,
                        reference,
                        intent,
                        NOW
                )
        );

        when(receptionService.receive(
                paymentId,
                reference,
                intent,
                NOW
        )).thenReturn(
                new PaymentWorkflowResult(
                        paymentId,
                        reference,
                        PaymentStatus.RECEIVED,
                        1L,
                        true
                )
        );

        PaymentInitiationOrchestrationService service =
                new PaymentInitiationOrchestrationService(
                        idempotencyPort,
                        preparationPort,
                        receptionService,
                        timeProvider
                );

        InitiateDebitResult result =
                service.initiateDebit(command);

        assertThat(result.paymentId())
                .isEqualTo(paymentId);
        assertThat(result.status())
                .isEqualTo(
                        PaymentStatus.RECEIVED
                );
        assertThat(result.optionalConfirmationChallenge())
                .isEmpty();
    }

    private static InitiateDebitCommand command() {
        return new InitiateDebitCommand(
                "TRESOR_PAY",
                "TRESOR_PAY",
                "TP_APP_001",
                "AVI-2025-00045678",
                new BigDecimal("600000"),
                "XAF",
                "10005-00001-12345678901-12",
                "Société ABC SARL",
                ClaimType.AVI,
                "100200300",
                NOW,
                List.of(
                        new InitiateDebitBeneficiaryCommand(
                                "10005-00001-TRESDGI-97",
                                new BigDecimal("600000")
                        )
                ),
                "https://tresorpay.cm/callback",
                "IDEMPOTENCY-00000001",
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                )
        );
    }
}
