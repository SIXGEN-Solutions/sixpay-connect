package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import com.sixpay.payment.application.port.output.PaymentTreasuryAccountResolutionPort;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.client.ConfirmationAccessTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Composes the Payment-owned Treasury adapter over the already-approved banking HTTP client. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TreasuryResolutionProperties.class)
@ConditionalOnProperty(prefix = "sixpay.payment.treasury", name = "enabled", havingValue = "true")
public class TreasuryResolutionConfiguration {

    @Bean
    TreasuryAccountLookupClient treasuryAccountLookupClient(
            @Qualifier("amplitudePaymentConfirmationRestClient") RestClient restClient,
            ConfirmationAccessTokenProvider tokenProvider
    ) {
        return new RestTreasuryAccountLookupClient(restClient, tokenProvider);
    }

    @Bean
    PaymentTreasuryAccountResolutionPort paymentTreasuryAccountResolutionPort(
            TreasuryResolutionProperties properties,
            TreasuryAccountLookupClient client
    ) {
        return new ConfiguredTreasuryAccountResolutionAdapter(properties, client);
    }
}
