package com.sixpay.accounting.infrastructure.tresorpay.configuration;

import com.sixpay.accounting.application.port.output.TresorPayPaymentStatusGateway;
import com.sixpay.accounting.configuration.AccountingModuleConfiguration;
import com.sixpay.accounting.infrastructure.tresorpay.client.OAuth2TresorPayStatusAccessTokenProvider;
import com.sixpay.accounting.infrastructure.tresorpay.client.RestTresorPayPaymentStatusClient;
import com.sixpay.accounting.infrastructure.tresorpay.client.TresorPayStatusAccessTokenProvider;
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
@EnableConfigurationProperties(TresorPayStatusProperties.class)
@ConditionalOnProperty(
        prefix = TresorPayStatusProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class TresorPayStatusConfiguration {

    @Bean
    TresorPayStatusAccessTokenProvider
    tresorPayStatusAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            TresorPayStatusProperties properties
    ) {
        return new OAuth2TresorPayStatusAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient tresorPayStatusRestClient(
            StandardRestClientFactory factory,
            TresorPayStatusProperties properties,
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
    @ConditionalOnMissingBean(TresorPayPaymentStatusGateway.class)
    TresorPayPaymentStatusGateway tresorPayPaymentStatusGateway(
            RestClient tresorPayStatusRestClient,
            TresorPayStatusAccessTokenProvider tokenProvider,
            TresorPayStatusProperties properties,
            @Qualifier(AccountingModuleConfiguration.ACCOUNTING_CLOCK)
            Clock accountingClock
    ) {
        return new RestTresorPayPaymentStatusClient(
                tresorPayStatusRestClient,
                tokenProvider,
                properties,
                accountingClock
        );
    }
}
