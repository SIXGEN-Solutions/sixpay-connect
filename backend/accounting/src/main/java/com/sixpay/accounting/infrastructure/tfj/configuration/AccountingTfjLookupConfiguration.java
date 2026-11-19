package com.sixpay.accounting.infrastructure.tfj.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccountingTfjLookupProperties.class)
@ConditionalOnProperty(
        prefix = AccountingTfjLookupProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AccountingTfjLookupConfiguration {



    @Bean("tfjLookupRestClient")
    RestClient tfjLookupRestClient(
            StandardRestClientFactory factory,
            AccountingTfjLookupProperties properties,
            SslBundles sslBundles
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(
                        properties.connectTimeout(),
                        properties.readTimeout()
                ),
                properties.security().mtlsEnabled()
                        ? sslBundles.getBundle(properties.security().sslBundle()).createSslContext()
                        : null,
                List.of(),
                properties.security().allowInsecureHttp()
        );
    }
}
