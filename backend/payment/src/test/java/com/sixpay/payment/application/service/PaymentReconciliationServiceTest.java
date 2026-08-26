package com.sixpay.payment.application.service;

import com.sixpay.payment.application.command.RecordTfjConfirmationCommand;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.EndOfDayConfirmationSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import com.sixpay.payment.domain.policy.UniqueTfjMatchProof;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentReconciliationServiceTest {

    @Test
    void delegatesOnlyTfjEvidenceToAggregate() {
        PaymentMutationCoordinator coordinator =
                Mockito.mock(PaymentMutationCoordinator.class);

        PaymentReconciliationService service =
                new PaymentReconciliationService(coordinator);

        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());
        EndOfDayConfirmationSnapshot evidence =
                Mockito.mock(
                        EndOfDayConfirmationSnapshot.class
                );
        UniqueTfjMatchProof matchProof =
                Mockito.mock(UniqueTfjMatchProof.class);
        PaymentFailure failure =
                Mockito.mock(PaymentFailure.class);
        PaymentPolicyBundle policies =
                Mockito.mock(PaymentPolicyBundle.class);
        Instant decisionAt =
                Instant.parse("2026-08-01T22:00:00Z");

        RecordTfjConfirmationCommand command =
                new RecordTfjConfirmationCommand(
                        paymentId,
                        evidence,
                        matchProof,
                        failure,
                        decisionAt,
                        policies
                );

        PaymentWorkflowResult expected =
                Mockito.mock(PaymentWorkflowResult.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Payment>> mutationCaptor =
                ArgumentCaptor.forClass(Consumer.class);

        when(coordinator.mutate(
                Mockito.eq(paymentId),
                Mockito.any()
        )).thenReturn(expected);

        PaymentWorkflowResult actual =
                service.reconcileTfj(command);

        assertThat(actual).isSameAs(expected);

        verify(coordinator).mutate(
                Mockito.eq(paymentId),
                mutationCaptor.capture()
        );

        Payment payment = Mockito.mock(Payment.class);
        mutationCaptor.getValue().accept(payment);

        verify(payment).recordMatchedEndOfDayConfirmation(
                evidence,
                matchProof,
                failure,
                decisionAt,
                policies
        );
    }
}
