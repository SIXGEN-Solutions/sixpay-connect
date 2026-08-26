package com.sixpay.accounting.infrastructure.accountingapi.configuration;

import com.sixpay.accounting.application.port.output.AccountingBatchGateway;
import com.sixpay.accounting.infrastructure.accountingapi.client.AccountingApiAccessTokenProvider;
import com.sixpay.accounting.infrastructure.accountingapi.client.OAuth2AccountingApiAccessTokenProvider;
import com.sixpay.accounting.infrastructure.accountingapi.client.RestAccountingBatchClient;
import com.sixpay.accounting.infrastructure.accountingapi.mapper.AccountingApiMapper;
import com.sixpay.accounting.infrastructure.accountingapi.validation.AccountingApiResponseValidator;
import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        AccountingApiProperties.class
)
@ConditionalOnProperty(
        prefix = AccountingApiProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AccountingApiConfiguration {

    @Bean
    AccountingApiAccessTokenProvider
    accountingApiAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AccountingApiProperties properties
    ) {
        return new OAuth2AccountingApiAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient accountingApiRestClient(
            StandardRestClientFactory factory,
            AccountingApiProperties properties,
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
    AccountingApiMapper accountingApiMapper() {
        return new AccountingApiMapper();
    }

    @Bean
    AccountingApiResponseValidator
    accountingApiResponseValidator() {
        return new AccountingApiResponseValidator();
    }

    @Bean
    @ConditionalOnMissingBean(AccountingBatchGateway.class)
    AccountingBatchGateway accountingBatchGateway(
            RestClient accountingApiRestClient,
            AccountingApiAccessTokenProvider tokenProvider,
            AccountingApiProperties properties,
            AccountingApiMapper mapper,
            AccountingApiResponseValidator validator,
            ObjectMapper objectMapper
    ) {
        return new RestAccountingBatchClient(
                accountingApiRestClient,
                tokenProvider,
                properties,
                mapper,
                validator,
                objectMapper
        );
    }
}
