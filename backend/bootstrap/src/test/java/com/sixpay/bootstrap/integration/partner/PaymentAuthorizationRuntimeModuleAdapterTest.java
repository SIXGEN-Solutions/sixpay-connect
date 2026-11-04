package com.sixpay.bootstrap.integration.partner;

import com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase;
import com.sixpay.customer.management.application.port.input.ResolvedCustomerSubscription;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.partner.application.view.PartnerIdentityView;
import com.sixpay.payment.domain.model.*;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.application.model.PartnerMachineIdentityView;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentAuthorizationRuntimeModuleAdapterTest {
    private static final UUID PARTNER = UUID.randomUUID();
    private static final UUID SUBSCRIPTION = UUID.randomUUID();
    private final PartnerMachineIdentityQueryUseCase machines = mock(PartnerMachineIdentityQueryUseCase.class);
    private final PartnerIdentityQueryUseCase partners = mock(PartnerIdentityQueryUseCase.class);
    private final ResolveCustomerSubscriptionUseCase subscriptions = mock(ResolveCustomerSubscriptionUseCase.class);
    private final PaymentAuthorizationRuntimeModuleAdapter adapter =
            new PaymentAuthorizationRuntimeModuleAdapter(machines, partners, subscriptions);

    private PaymentState state() {
        PaymentState state = mock(PaymentState.class);
        PaymentInitiationContext context = mock(PaymentInitiationContext.class);
        DebtorAccountReference account = mock(DebtorAccountReference.class);
        when(state.initiationContext()).thenReturn(Optional.of(context));
        when(context.applicationId()).thenReturn("partner-A");
        when(context.partnerLoginName()).thenReturn("machine-A1");
        when(state.externalSubscriptionReference())
                .thenReturn(ExternalSubscriptionReference.of(SUBSCRIPTION.toString()));
        when(state.optionalDebtorAccountReference()).thenReturn(Optional.of(account));
        when(state.financialInstitutionCode()).thenReturn(FinancialInstitutionCode.of("BANK_CM"));
        when(account.bindingFingerprint()).thenReturn("v1:" + "a".repeat(64));
        return state;
    }

    private void registered() {
        PartnerIdentityView partner = mock(PartnerIdentityView.class);
        when(partner.partnerId()).thenReturn(PARTNER);
        when(partner.acceptsNewTransactions()).thenReturn(true);
        when(partners.findByPartnerIdentifier("partner-A")).thenReturn(Optional.of(partner));
        when(machines.findByMachineSubject("machine-A1"))
                .thenReturn(Optional.of(new PartnerMachineIdentityView("machine-A1", "partner-A")));
    }

    @Test void matchingAppAndSubscriptionAuthorize() {
        registered();
        ResolvedCustomerSubscription subscription = mock(ResolvedCustomerSubscription.class);
        when(subscription.subscriptionId()).thenReturn(SUBSCRIPTION);
        when(subscription.partnerId()).thenReturn(PARTNER);
        when(subscription.financialInstitutionCode()).thenReturn("BANK_CM");
        when(subscription.accountBindingFingerprint()).thenReturn("v1:" + "a".repeat(64));
        when(subscriptions.resolve(SUBSCRIPTION, PARTNER)).thenReturn(subscription);
        var result = adapter.resolve(state());
        assertEquals(Boolean.TRUE, result.applicationAuthorized());
        assertEquals(Boolean.TRUE, result.subscriptionAuthorized());
    }

    @Test void differentApplicationIsRejectedBeforeSubscriptionLookup() {
        registered();
        when(machines.findByMachineSubject("machine-A1"))
                .thenReturn(Optional.of(new PartnerMachineIdentityView("machine-A1", "partner-B")));
        var result = adapter.resolve(state());
        assertEquals(Boolean.FALSE, result.applicationAuthorized());
        verifyNoInteractions(subscriptions);
    }

    @Test void suspendedSubscriptionBlocksPostingButNotOtpVerification() {
        registered();
        when(subscriptions.resolve(SUBSCRIPTION, PARTNER))
                .thenThrow(new CustomerDomainException("customer subscription unavailable"));
        var result = adapter.resolve(state());
        assertEquals(Boolean.TRUE, result.applicationAuthorized());
        assertEquals(Boolean.FALSE, result.subscriptionAuthorized());
    }
}
