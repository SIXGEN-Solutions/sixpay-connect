package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.partner.AuthenticatedPartnerCallerPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentConfirmationPartnerAccessGuardTest {
    private static final String REFERENCE = "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private final AuthenticatedPartnerCallerPort caller = mock(AuthenticatedPartnerCallerPort.class);
    private final PartnerIdentityAlignmentService alignment = mock(PartnerIdentityAlignmentService.class);
    private final PaymentLookupPort payments = mock(PaymentLookupPort.class);
    private final PaymentConfirmationPartnerAccessGuard guard =
            new PaymentConfirmationPartnerAccessGuard(caller, alignment, payments);

    private void paymentBelongsTo(String owner) {
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        PaymentInitiationContext context = mock(PaymentInitiationContext.class);
        when(payments.findByPublicPaymentReference(PublicPaymentReference.of(REFERENCE)))
                .thenReturn(Optional.of(payment));
        when(payment.toState()).thenReturn(state);
        when(state.initiationContext()).thenReturn(Optional.of(context));
        when(context.applicationId()).thenReturn(owner);
        when(context.partnerLoginName()).thenReturn("client-a");
    }

    @Test void missingMachineIdentityDeniesWithoutPaymentRead() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.empty());
        assertThat(guard.isAllowed(REFERENCE)).isFalse();
        verifyNoInteractions(payments, alignment);
    }

    @Test void missingPaymentDeniesWithoutPartnerResolution() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.of("client-a"));
        when(payments.findByPublicPaymentReference(PublicPaymentReference.of(REFERENCE)))
                .thenReturn(Optional.empty());
        assertThat(guard.isAllowed(REFERENCE)).isFalse();
        verifyNoInteractions(alignment);
    }

    @Test void matchingResolvedPartnerAllowsAccess() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.of("client-a"));
        paymentBelongsTo("partner-a");
        // A resolved identity is returned for an authorized registered Partner.
        when(alignment.requireConsistentPartner(eq("client-a"), eq("partner-a")))
                .thenReturn(new com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity(
                        java.util.UUID.randomUUID(), "partner-a", true));
        assertThat(guard.isAllowed(REFERENCE)).isTrue();
    }

    @Test void differentPartnerDeniesAccess() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.of("client-b"));
        paymentBelongsTo("partner-a");
        when(alignment.requireConsistentPartner(eq("client-b"), eq("partner-a")))
                .thenThrow(new PartnerIdentityResolutionException(
                        PartnerIdentityResolutionFailure.PARTNER_IDENTITY_MISMATCH,
                        "Mismatch"));
        assertThat(guard.isAllowed(REFERENCE)).isFalse();
    }

    @Test void samePartnerDifferentTechnicalApplicationDeniedBeforeResolution() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.of("client-b"));
        paymentBelongsTo("partner-a");
        assertThat(guard.isAllowed(REFERENCE)).isFalse();
        verifyNoInteractions(alignment);
    }

    @Test void missingTrustedInitiatorBindingFailsClosed() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.of("client-a"));
        Payment payment = mock(Payment.class);
        PaymentState state = mock(PaymentState.class);
        PaymentInitiationContext context = mock(PaymentInitiationContext.class);
        when(payments.findByPublicPaymentReference(PublicPaymentReference.of(REFERENCE)))
                .thenReturn(Optional.of(payment));
        when(payment.toState()).thenReturn(state);
        when(state.initiationContext()).thenReturn(Optional.of(context));
        when(context.applicationId()).thenReturn("partner-a");
        assertThat(guard.isAllowed(REFERENCE)).isFalse();
        verifyNoInteractions(alignment);
    }

    @Test void unauthorizedPartnerDeniesAccess() {
        when(caller.currentAuthenticatedSubject()).thenReturn(Optional.of("client-a"));
        paymentBelongsTo("partner-a");
        when(alignment.requireConsistentPartner(eq("client-a"), eq("partner-a")))
                .thenThrow(new PartnerIdentityResolutionException(
                        PartnerIdentityResolutionFailure.PARTNER_NOT_AUTHORIZED,
                        "Inactive"));
        assertThat(guard.isAllowed(REFERENCE)).isFalse();
    }
}
