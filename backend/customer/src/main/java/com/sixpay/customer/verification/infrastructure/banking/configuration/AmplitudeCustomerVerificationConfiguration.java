package com.sixpay.customer.verification.infrastructure.banking.configuration;

import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.infrastructure.banking.AmplitudeCustomerVerificationAdapter;
import com.sixpay.customer.verification.infrastructure.banking.client.*;
import com.sixpay.customer.verification.infrastructure.banking.error.*;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;
import com.sixpay.customer.verification.infrastructure.banking.observability.BankingVerificationObservation;
import com.sixpay.customer.verification.infrastructure.banking.retry.*;
import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BankingVerificationProperties.class)
@ConditionalOnProperty(
        prefix = BankingVerificationProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudeCustomerVerificationConfiguration {

    @Bean
    RestClient amplitudeCustomerVerificationRestClient(
            StandardRestClientFactory factory,
            BankingVerificationProperties properties
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(
                        properties.connectTimeout(),
                        properties.readTimeout()
                ),
                null,
                List.of(),
                true
        );
    }

    @Bean
    AmplitudeResponseValidator amplitudeResponseValidator() {
        return new AmplitudeResponseValidator();
    }

    @Bean
    AmplitudeCustomerVerificationMapper amplitudeCustomerVerificationMapper(
            BankingVerificationProperties properties
    ) {
        return new AmplitudeCustomerVerificationMapper(
                properties.evidenceTtl()
        );
    }

    @Bean
    com.sixpay.customer.management.infrastructure.banking.amplitude.AmplitudeCustomerLookupClient
            amplitudeCustomerLookupClient(
                    RestClient amplitudeCustomerVerificationRestClient) {
        return new com.sixpay.customer.management.infrastructure.banking.amplitude.RestAmplitudeCustomerLookupClient(
                amplitudeCustomerVerificationRestClient);
    }

    @Bean
    AmplitudeCustomerVerificationClient amplitudeCustomerVerificationClient(
            RestClient amplitudeCustomerVerificationRestClient,
            BankingVerificationProperties properties,
            ObjectMapper objectMapper
    ) {
        return new AmplitudeCustomerVerificationClient(
                amplitudeCustomerVerificationRestClient,
                properties,
                objectMapper
        );
    }

    @Bean
    BankingVerificationErrorClassifier bankingVerificationErrorClassifier() {
        return new BankingVerificationErrorClassifier();
    }

    @Bean
    AmplitudeCustomerVerificationAdapter amplitudeCustomerVerificationAdapter(
            AmplitudeCustomerVerificationClient client,
            AmplitudeCustomerVerificationMapper mapper,
            AmplitudeResponseValidator validator,
            BankingVerificationErrorClassifier classifier
    ) {
        return new AmplitudeCustomerVerificationAdapter(
                client,
                mapper,
                validator,
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
