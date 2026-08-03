package com.sixpay.customer.verification.infrastructure.banking.configuration;

import com.sixpay.customer.verification.application.port.out.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.infrastructure.banking.AmplitudeCustomerVerificationAdapter;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.client.CoreBankingAccessTokenProvider;
import com.sixpay.customer.verification.infrastructure.banking.client.OAuth2CoreBankingAccessTokenProvider;
import com.sixpay.customer.verification.infrastructure.banking.error.BankingVerificationErrorClassifier;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;
import com.sixpay.customer.verification.infrastructure.banking.observability.BankingVerificationObservation;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetrySleeper;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetryingBankingCustomerVerificationAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class AmplitudeCustomerVerificationConfiguration {

    @Bean
    CoreBankingAccessTokenProvider coreBankingAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            BankingVerificationProperties properties
    ) {
        return new OAuth2CoreBankingAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient amplitudeCustomerVerificationRestClient(
            RestClient.Builder builder,
            BankingVerificationProperties properties,
            SslBundles sslBundles
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .sslContext(
                        sslBundles.getBundle(
                                properties.security().sslBundle()
                        ).createSslContext()
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    AmplitudeCustomerVerificationMapper
            amplitudeCustomerVerificationMapper(
                    BankingVerificationProperties properties
            ) {
        return new AmplitudeCustomerVerificationMapper(
                properties.evidenceTtl()
        );
    }

    @Bean
    AmplitudeCustomerVerificationClient
            amplitudeCustomerVerificationClient(
                    RestClient amplitudeCustomerVerificationRestClient,
                    CoreBankingAccessTokenProvider tokenProvider,
                    BankingVerificationProperties properties,
                    ObjectMapper objectMapper
            ) {
        return new AmplitudeCustomerVerificationClient(
                amplitudeCustomerVerificationRestClient,
                tokenProvider,
                properties,
                objectMapper
        );
    }

    @Bean
    BankingVerificationErrorClassifier
            bankingVerificationErrorClassifier() {
        return new BankingVerificationErrorClassifier();
    }

    @Bean
    AmplitudeCustomerVerificationAdapter
            amplitudeCustomerVerificationAdapter(
                    AmplitudeCustomerVerificationClient client,
                    AmplitudeCustomerVerificationMapper mapper,
                    BankingVerificationErrorClassifier classifier
            ) {
        return new AmplitudeCustomerVerificationAdapter(
                client,
                mapper,
                classifier
        );
    }

    @Bean
    RetrySleeper bankingVerificationRetrySleeper() {
        return RetrySleeper.threadSleep();
    }

    @Bean
    BankingVerificationObservation bankingVerificationObservation(
            MeterRegistry meterRegistry
    ) {
        return new BankingVerificationObservation(meterRegistry);
    }

    @Bean
    @Primary
    BankingCustomerVerificationPort bankingCustomerVerificationPort(
            AmplitudeCustomerVerificationAdapter delegate,
            BankingVerificationProperties properties,
            RetrySleeper sleeper,
            BankingVerificationObservation observation
    ) {
        return new RetryingBankingCustomerVerificationAdapter(
                delegate,
                properties,
                sleeper,
                observation
        );
    }
}
