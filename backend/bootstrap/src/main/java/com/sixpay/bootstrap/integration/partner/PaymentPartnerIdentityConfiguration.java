package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.partner.application.port.input.PartnerQueryUseCase;
import com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase;
import com.sixpay.payment.application.port.PartnerAuthorizationPort;
import com.sixpay.payment.application.port.PaymentAuthorizationRuntimePort;
import com.sixpay.payment.application.port.output.partner.AuthenticatedPartnerCallerPort;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.service.PartnerIdentityAlignmentService;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross-module wiring only.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentPartnerIdentityConfiguration {


    @Bean
    AuthenticatedPartnerCallerPort authenticatedPartnerCallerPort(
            CurrentMachineIdentityProvider machineIdentityProvider
    ) {
        return new SecurityAuthenticatedPartnerCallerAdapter(
                machineIdentityProvider
        );
    }

    @Bean
    PartnerIdentityResolutionPort paymentPartnerIdentityResolutionPort(
            CurrentMachineIdentityProvider machineIdentityProvider,
            PartnerMachineIdentityQueryUseCase machineIdentityQuery,
            PartnerIdentityQueryUseCase partnerIdentityQueryUseCase
    ) {
        return new PaymentPartnerIdentityModuleAdapter(
                machineIdentityProvider,
                machineIdentityQuery,
                partnerIdentityQueryUseCase
        );
    }

    @Bean
    PartnerAuthorizationPort paymentPartnerAuthorizationPort(
            PartnerIdentityQueryUseCase identities,
            PartnerQueryUseCase partners) {
        return new PaymentPartnerAuthorizationModuleAdapter(identities, partners);
    }

    @Bean
    PaymentAuthorizationRuntimePort paymentAuthorizationRuntimePort(
            PartnerMachineIdentityQueryUseCase machines,
            PartnerIdentityQueryUseCase partners,
            ResolveCustomerSubscriptionUseCase subscriptions) {
        return new PaymentAuthorizationRuntimeModuleAdapter(machines, partners, subscriptions);
    }

    @Bean
    PartnerIdentityAlignmentService partnerIdentityAlignmentService(
            PartnerIdentityResolutionPort resolutionPort
    ) {
        return new PartnerIdentityAlignmentService(resolutionPort);
    }
}
