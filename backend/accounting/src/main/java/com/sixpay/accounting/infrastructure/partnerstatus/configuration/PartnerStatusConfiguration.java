package com.sixpay.accounting.infrastructure.partnerstatus.configuration;

import com.sixpay.accounting.application.port.output.PartnerExternalPaymentStatusGateway;
import com.sixpay.accounting.configuration.AccountingModuleConfiguration;
import com.sixpay.accounting.infrastructure.partnerstatus.client.OAuth2PartnerStatusAccessTokenProvider;
import com.sixpay.accounting.infrastructure.partnerstatus.client.RestPartnerExternalPaymentStatusClient;
import com.sixpay.accounting.infrastructure.partnerstatus.client.PartnerStatusAccessTokenProvider;
import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PartnerStatusProperties.class)
@ConditionalOnProperty(
        prefix = PartnerStatusProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class PartnerStatusConfiguration {

    @Bean
    PartnerStatusAccessTokenProvider
    partnerStatusAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            PartnerStatusProperties properties
    ) {
        return new OAuth2PartnerStatusAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient partnerStatusRestClient(
            StandardRestClientFactory factory,
            PartnerStatusProperties properties,
            SslBundles sslBundles
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(
                        properties.connectTimeout(),
                        properties.readTimeout()
                ),
                sslBundles.getBundle(
                        properties.security().sslBundle()
                ).createSslContext(),
                List.of()
        );
    }

    @Bean
    @ConditionalOnMissingBean(PartnerExternalPaymentStatusGateway.class)
    PartnerExternalPaymentStatusGateway partnerExternalPaymentStatusGateway(
            RestClient partnerStatusRestClient,
            PartnerStatusAccessTokenProvider tokenProvider,
            PartnerStatusProperties properties,
            @Qualifier(AccountingModuleConfiguration.ACCOUNTING_CLOCK)
            Clock accountingClock
    ) {
        return new RestPartnerExternalPaymentStatusClient(
                partnerStatusRestClient,
                tokenProvider,
                properties,
                accountingClock
        );
    }
}
