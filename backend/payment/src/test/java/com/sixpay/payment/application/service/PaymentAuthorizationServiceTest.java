package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.application.port.PartnerAuthorizationPort;
import com.sixpay.payment.domain.policy.PartnerAuthorizationView;
import com.sixpay.payment.domain.policy.SixpayAuthorizationGate;
import com.sixpay.payment.domain.policy.SixpayAuthorizationGateResult;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentAuthorizationServiceTest {

    @Test
    void verifiedOtpTriggersGateAfterAuthorizationCheckingTransition() {
        PaymentMutationCoordinator coordinator =
                mock(PaymentMutationCoordinator.class);
        SixpayAuthorizationGate gate =
                mock(SixpayAuthorizationGate.class);
        SixpayAuthorizationGateResult gateResult =
                mock(SixpayAuthorizationGateResult.class);
        PartnerAuthorizationPort partnerAuthorizationPort =
                mock(PartnerAuthorizationPort.class);
        PartnerAuthorizationView partnerAuthorization =
                mock(PartnerAuthorizationView.class);
        PaymentInitiationContext initiationContext =
                mock(PaymentInitiationContext.class);

        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        ConfirmationChallenge verifiedChallenge =
                mock(ConfirmationChallenge.class);

        PaymentId paymentId = mock(PaymentId.class);
        PublicPaymentReference publicReference =
                mock(PublicPaymentReference.class);

        when(payment.id()).thenReturn(paymentId);
        when(payment.publicPaymentReference())
                .thenReturn(publicReference);
        when(payment.status())
                .thenReturn(PaymentStatus.AUTHORIZATION_CHECKING);
        when(payment.businessVersion()).thenReturn(7L);
        when(payment.toState()).thenReturn(state);
        when(verifiedChallenge.optionalVerifiedAt())
                .thenReturn(Optional.of(
                        Instant.parse("2026-09-04T20:00:00Z")
                ));
        when(state.initiationContext())
                .thenReturn(Optional.of(initiationContext));
        when(partnerAuthorizationPort.resolve(initiationContext))
                .thenReturn(partnerAuthorization);

        when(gate.evaluate(state, partnerAuthorization))
                .thenReturn(gateResult);
        when(gateResult.rejected()).thenReturn(false);
        when(gateResult.incomplete()).thenReturn(false);

        when(coordinator.mutate(eq(paymentId), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<Payment> mutation =
                            invocation.getArgument(1);

                    mutation.accept(payment);

                    return new PaymentWorkflowResult(
                            paymentId,
                            publicReference,
                            PaymentStatus.FUNDS_CONTROL_PENDING,
                            7L,
                            true
                    );
                });

        PaymentAuthorizationService service =
                new PaymentAuthorizationService(
                        coordinator,
                        gate,
                        partnerAuthorizationPort
                );

        PaymentWorkflowResult result =
                service.startAuthorization(
                        paymentId,
                        verifiedChallenge
                );

        assertEquals(
                PaymentStatus.FUNDS_CONTROL_PENDING,
                result.status()
        );

        InOrder order = inOrder(
                payment,
                partnerAuthorizationPort,
                gate
        );
        order.verify(payment)
                .recordCustomerConfirmation(verifiedChallenge);
        order.verify(payment).toState();
        order.verify(partnerAuthorizationPort)
                .resolve(initiationContext);
        order.verify(gate)
                .evaluate(state, partnerAuthorization);
        order.verify(payment)
                .approveSixpayAuthorization(any());
    }

    @Test
    void incompleteGateKeepsPaymentInAuthorizationChecking() {
        PaymentMutationCoordinator coordinator =
                mock(PaymentMutationCoordinator.class);
        SixpayAuthorizationGate gate =
                mock(SixpayAuthorizationGate.class);
        SixpayAuthorizationGateResult gateResult =
                mock(SixpayAuthorizationGateResult.class);
        PartnerAuthorizationPort partnerAuthorizationPort =
                mock(PartnerAuthorizationPort.class);
        PartnerAuthorizationView partnerAuthorization =
                mock(PartnerAuthorizationView.class);
        PaymentInitiationContext initiationContext =
                mock(PaymentInitiationContext.class);

        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        ConfirmationChallenge verifiedChallenge =
                mock(ConfirmationChallenge.class);
        PaymentId paymentId = mock(PaymentId.class);
        PublicPaymentReference publicReference =
                mock(PublicPaymentReference.class);

        when(payment.id()).thenReturn(paymentId);
        when(payment.publicPaymentReference())
                .thenReturn(publicReference);
        when(payment.status())
                .thenReturn(PaymentStatus.AUTHORIZATION_CHECKING);
        when(payment.businessVersion()).thenReturn(7L);
        when(payment.toState()).thenReturn(state);
        when(state.initiationContext())
                .thenReturn(Optional.of(initiationContext));
        when(partnerAuthorizationPort.resolve(initiationContext))
                .thenReturn(partnerAuthorization);
        when(gate.evaluate(state, partnerAuthorization))
                .thenReturn(gateResult);
        when(gateResult.rejected()).thenReturn(false);
        when(gateResult.incomplete()).thenReturn(true);

        when(coordinator.mutate(eq(paymentId), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<Payment> mutation =
                            invocation.getArgument(1);

                    mutation.accept(payment);

                    return new PaymentWorkflowResult(
                            paymentId,
                            publicReference,
                            PaymentStatus.AUTHORIZATION_CHECKING,
                            7L,
                            true
                    );
                });

        PaymentAuthorizationService service =
                new PaymentAuthorizationService(
                        coordinator,
                        gate,
                        partnerAuthorizationPort
                );

        PaymentWorkflowResult result =
                service.startAuthorization(
                        paymentId,
                        verifiedChallenge
                );

        assertEquals(
                PaymentStatus.AUTHORIZATION_CHECKING,
                result.status()
        );
        verify(payment, never())
                .approveSixpayAuthorization(any());
    }

    @Test
    void rejectedGateResultStopsAuthorizationFlow() {
        PaymentMutationCoordinator coordinator =
                mock(PaymentMutationCoordinator.class);
        SixpayAuthorizationGate gate =
                mock(SixpayAuthorizationGate.class);
        SixpayAuthorizationGateResult gateResult =
                mock(SixpayAuthorizationGateResult.class);
        PartnerAuthorizationPort partnerAuthorizationPort =
                mock(PartnerAuthorizationPort.class);
        PartnerAuthorizationView partnerAuthorization =
                mock(PartnerAuthorizationView.class);
        PaymentInitiationContext initiationContext =
                mock(PaymentInitiationContext.class);

        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        ConfirmationChallenge verifiedChallenge =
                mock(ConfirmationChallenge.class);
        PaymentId paymentId = mock(PaymentId.class);

        when(payment.toState()).thenReturn(state);
        when(state.initiationContext())
                .thenReturn(Optional.of(initiationContext));
        when(partnerAuthorizationPort.resolve(initiationContext))
                .thenReturn(partnerAuthorization);
        when(gate.evaluate(state, partnerAuthorization))
                .thenReturn(gateResult);
        when(gateResult.rejected()).thenReturn(true);

        when(coordinator.mutate(eq(paymentId), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<Payment> mutation =
                            invocation.getArgument(1);

                    mutation.accept(payment);
                    throw new AssertionError(
                            "Rejected gate should stop before workflow result"
                    );
                });

        PaymentAuthorizationService service =
                new PaymentAuthorizationService(
                        coordinator,
                        gate,
                        partnerAuthorizationPort
                );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> service.startAuthorization(
                        paymentId,
                        verifiedChallenge
                )
        );

        InOrder order = inOrder(
                payment,
                partnerAuthorizationPort,
                gate
        );
        order.verify(payment)
                .recordCustomerConfirmation(verifiedChallenge);
        order.verify(payment).toState();
        order.verify(partnerAuthorizationPort)
                .resolve(initiationContext);
        order.verify(gate)
                .evaluate(state, partnerAuthorization);
    }
}
