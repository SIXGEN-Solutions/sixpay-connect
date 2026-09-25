package com.sixpay.bootstrap.integration.partner;

import com.sixpay.partner.application.port.input.PartnerIdentityQueryUseCase;
import com.sixpay.payment.application.port.output.partner.PartnerIdentityResolutionPort;
import com.sixpay.payment.application.service.PartnerIdentityAlignmentService;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross-module wiring only.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentPartnerIdentityConfiguration {

    @Bean
    PartnerIdentityResolutionPort paymentPartnerIdentityResolutionPort(
            CurrentUserProvider currentUserProvider,
            PartnerIdentityQueryUseCase partnerIdentityQueryUseCase
    ) {
        return new PaymentPartnerIdentityModuleAdapter(
                currentUserProvider,
                partnerIdentityQueryUseCase
        );
    }

    @Bean
    PartnerIdentityAlignmentService partnerIdentityAlignmentService(
            PartnerIdentityResolutionPort resolutionPort
    ) {
        return new PartnerIdentityAlignmentService(
                resolutionPort
        );
    }
}
