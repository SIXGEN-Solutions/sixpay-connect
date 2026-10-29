package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.customer.PaymentCustomerSubscriptionPort;
import com.sixpay.payment.application.port.output.customer.ResolvedPaymentSubscription;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.port.output.partner.ResolvedPartnerIdentity;
import com.sixpay.payment.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** P3.2: scenarios fonctionnels a travers les ports Payment -> Partner -> Customer. */
class PaymentCustomerVerificationIntermoduleTest {
    private static final UUID SUBSCRIPTION = UUID.fromString("7a7185d1-3c0a-43e0-8b64-a9407f910001");
    private static final UUID OTHER_SUBSCRIPTION = UUID.fromString("7a7185d1-3c0a-43e0-8b64-a9407f910002");
    private static final UUID PARTNER = UUID.fromString("7a7185d1-3c0a-43e0-8b64-a9407f910003");
    private static final UUID OTHER_PARTNER = UUID.fromString("7a7185d1-3c0a-43e0-8b64-a9407f910004");
    private static final UUID PAYMENT_ID = UUID.fromString("7a7185d1-3c0a-43e0-8b64-a9407f910005");
    private static final UUID VERIFICATION_ID = UUID.fromString("7a7185d1-3c0a-43e0-8b64-a9407f910006");
    private static final Instant NOW = Instant.parse("2026-08-03T10:30:00Z");
    private static final Instant DEADLINE = NOW.plusSeconds(30);

    private final PaymentCustomerSubscriptionPort subscriptions = mock(PaymentCustomerSubscriptionPort.class);
    private final PartnerIdentityResolutionPort partners = mock(PartnerIdentityResolutionPort.class);
    private final Payment payment = mock(Payment.class);
    private final PaymentState state = mock(PaymentState.class);
    private final PaymentCustomerVerificationRequestFactory factory =
            new PaymentCustomerVerificationRequestFactory(subscriptions, partners);

    @BeforeEach
    void preparePaymentState() {
        when(payment.status()).thenReturn(PaymentStatus.BANKING_VERIFICATION_PENDING);
        when(payment.toState()).thenReturn(state);
        when(state.initiationContext()).thenReturn(Optional.of(new PaymentInitiationContext(
                "DISPLAY_ONLY", "PARTNER_APP_001", "Client", ClaimType.AVI,
                "100200300", NOW, CallbackEndpoint.of("https://example.org/callback"))));
        when(state.externalSubscriptionReference()).thenReturn(ExternalSubscriptionReference.of(SUBSCRIPTION.toString()));
        when(state.requestIdentity()).thenReturn(mock(PaymentRequestIdentity.class));
        when(state.requestIdentity().correlationId()).thenReturn(CorrelationId.of("11111111-1111-1111-1111-111111111111"));
        when(state.paymentId()).thenReturn(new PaymentId(PAYMENT_ID));
        when(partners.findByPartnerIdentifier("PARTNER_APP_001"))
                .thenReturn(Optional.of(new ResolvedPartnerIdentity(PARTNER, "PARTNER_APP_001", true)));
        when(subscriptions.resolve(SUBSCRIPTION, PARTNER)).thenReturn(subscription(SUBSCRIPTION, PARTNER));
    }

    @Test
    void propagatesCanonicalSubscriptionAndBankingIdentifiers() {
        CustomerVerificationRequest request = invoke();
        verify(partners).findByPartnerIdentifier("PARTNER_APP_001");
        verify(subscriptions).resolve(SUBSCRIPTION, PARTNER);
        assertThat(request.verificationId()).isEqualTo(VERIFICATION_ID);
        assertThat(request.customerNiu()).isEqualTo("100200300");
        assertThat(request.customerLegalName()).isEqualTo("Client de test");
        assertThat(request.financialInstitutionCode()).isEqualTo("REG");
        assertThat(request.bankingCustomerReference()).isEqualTo("BANK-CUSTOMER-001");
        assertThat(request.customerNumber()).isEqualTo("CUSTOMER-001");
        assertThat(request.integrationAccountToken()).isEqualTo("BANK-ACCOUNT-001");
        assertThat(request.accountBindingFingerprint()).isEqualTo("binding-fingerprint");
        assertThat(request.causationId()).isEqualTo(PAYMENT_ID);
        assertThat(request.correlationId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(request.requestedAt()).isEqualTo(NOW);
        assertThat(request.deadlineAt()).isEqualTo(DEADLINE);
    }

    @Test
    void rejectsMissingPartnerBeforeResolvingCustomer() {
        when(partners.findByPartnerIdentifier("PARTNER_APP_001")).thenReturn(Optional.empty());
        assertThatThrownBy(this::invoke).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Partner is unavailable");
        verifyNoInteractions(subscriptions);
    }

    @Test
    void rejectsPartnerNotAuthorizedForNewTransactions() {
        when(partners.findByPartnerIdentifier("PARTNER_APP_001"))
                .thenReturn(Optional.of(new ResolvedPartnerIdentity(PARTNER, "PARTNER_APP_001", false)));
        assertThatThrownBy(this::invoke).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Partner is not authorized");
        verifyNoInteractions(subscriptions);
    }

    @Test
    void rejectsSubscriptionBelongingToAnotherPartner() {
        when(subscriptions.resolve(SUBSCRIPTION, PARTNER)).thenReturn(subscription(SUBSCRIPTION, OTHER_PARTNER));
        assertThatThrownBy(this::invoke).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CustomerSubscription Partner mismatch");
    }

    @Test
    void rejectsDifferentSubscriptionReturnedByCustomer() {
        when(subscriptions.resolve(SUBSCRIPTION, PARTNER)).thenReturn(subscription(OTHER_SUBSCRIPTION, PARTNER));
        assertThatThrownBy(this::invoke).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CustomerSubscription reference mismatch");
    }

    @Test
    void rejectsMissingSubscriptionWithoutProducingBankingRequest() {
        when(subscriptions.resolve(SUBSCRIPTION, PARTNER)).thenThrow(new IllegalStateException("Subscription unavailable"));
        assertThatThrownBy(this::invoke).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Subscription unavailable");
    }

    @Test
    void rejectsVerificationOutsidePendingStateBeforePartnerLookup() {
        when(payment.status()).thenReturn(PaymentStatus.RECEIVED);
        assertThatThrownBy(this::invoke).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BANKING_VERIFICATION_PENDING");
        verifyNoInteractions(partners, subscriptions);
    }

    private CustomerVerificationRequest invoke() {
        return factory.from(payment, VERIFICATION_ID, NOW, DEADLINE);
    }

    private static ResolvedPaymentSubscription subscription(UUID id, UUID partnerId) {
        return new ResolvedPaymentSubscription(id, partnerId, "REG", "BANK-CUSTOMER-001",
                "CUSTOMER-001", "100200300", "Client de test", "BANK-ACCOUNT-001",
                "binding-fingerprint", "***0001");
    }
}
