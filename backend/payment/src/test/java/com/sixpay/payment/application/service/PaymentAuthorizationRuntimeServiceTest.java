package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.PaymentAuthorizationRuntimePort;
import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.policy.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentAuthorizationRuntimeServiceTest {
    @Test void deniedSubscriptionPreservesVerifiedOtpButNeverAdvancesToFunds() {
        PaymentMutationCoordinator coordinator = mock(PaymentMutationCoordinator.class);
        PaymentAuthorizationRuntimePort runtime = mock(PaymentAuthorizationRuntimePort.class);
        PaymentAuthorizationService service = new PaymentAuthorizationService(
                coordinator, new SixpayAuthorizationGate(), Optional.empty(), Optional.of(runtime));
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        PaymentId id = mock(PaymentId.class);
        PublicPaymentReference reference = mock(PublicPaymentReference.class);
        ConfirmationChallenge challenge = mock(ConfirmationChallenge.class);
        when(payment.toState()).thenReturn(state);
        when(state.initiationContext()).thenReturn(Optional.empty());
        when(runtime.resolve(state)).thenReturn(new PaymentAuthorizationRuntimeView(false, true));
        when(coordinator.mutate(eq(id), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked") Consumer<Payment> mutation = invocation.getArgument(1);
            mutation.accept(payment);
            return new PaymentWorkflowResult(id, reference, PaymentStatus.AUTHORIZATION_CHECKING, 3L, true);
        });
        assertEquals(PaymentStatus.AUTHORIZATION_CHECKING,
                service.startAuthorization(id, challenge).status());
        verify(payment).recordCustomerConfirmation(challenge);
        verify(payment, never()).approveSixpayAuthorization(any());
    }
}
