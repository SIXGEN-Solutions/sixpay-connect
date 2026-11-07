package com.sixpay.bootstrap.integration.partner;

import com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.payment.application.port.PaymentAuthorizationRuntimePort;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.policy.PaymentAuthorizationRuntimeView;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;

import java.util.Objects;
import java.util.UUID;

/**
 * Cross-module composition only. Security owns machine->Partner registration;
 * Customer owns CustomerSubscription eligibility. No JPA cross-module access.
 */
public final class PaymentAuthorizationRuntimeModuleAdapter
        implements PaymentAuthorizationRuntimePort {
    private final PartnerMachineIdentityQueryUseCase machines;
    private final PartnerIdentityQueryUseCase partners;
    private final ResolveCustomerSubscriptionUseCase subscriptions;

    public PaymentAuthorizationRuntimeModuleAdapter(
            PartnerMachineIdentityQueryUseCase machines,
            PartnerIdentityQueryUseCase partners,
            ResolveCustomerSubscriptionUseCase subscriptions) {
        this.machines = Objects.requireNonNull(machines);
        this.partners = Objects.requireNonNull(partners);
        this.subscriptions = Objects.requireNonNull(subscriptions);
    }

    @Override
    public PaymentAuthorizationRuntimeView resolve(PaymentState state) {
        Objects.requireNonNull(state);
        var context = state.initiationContext().orElse(null);
        if (context == null || context.applicationId() == null
                || context.partnerLoginName() == null
                || context.partnerLoginName().isBlank()) {
            return new PaymentAuthorizationRuntimeView(null, null);
        }
        var identity = partners.findByPartnerIdentifier(context.applicationId()).orElse(null);
        var machine = machines.findByMachineSubject(context.partnerLoginName()).orElse(null);
        // A revoked/unregistered/mismatched machine cannot authorize this Payment.
        if (identity == null || machine == null
                || !context.applicationId().equals(machine.partnerIdentifier())
                || !context.partnerLoginName().equals(machine.machineSubject())
                || !identity.acceptsNewTransactions()) {
            return new PaymentAuthorizationRuntimeView(null, Boolean.FALSE);
        }
        try {
            UUID subscriptionId = UUID.fromString(state.externalSubscriptionReference().value());
            var subscription = subscriptions.resolve(subscriptionId, identity.partnerId());
            boolean matches = subscriptionId.equals(subscription.subscriptionId())
                    && identity.partnerId().equals(subscription.partnerId())
                    && state.optionalDebtorAccountReference().isPresent()
                    && state.financialInstitutionCode() != null
                    && state.financialInstitutionCode().value().equals(subscription.financialInstitutionCode())
                    && state.optionalDebtorAccountReference().orElseThrow().bindingFingerprint()
                            .equals(subscription.accountBindingFingerprint());
            return new PaymentAuthorizationRuntimeView(matches, Boolean.TRUE);
        } catch (CustomerDomainException | IllegalArgumentException denied) {
            // Includes suspended/closed/foreign subscriptions; disclose no existence.
            return new PaymentAuthorizationRuntimeView(Boolean.FALSE, Boolean.TRUE);
        }
    }
}
