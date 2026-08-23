package com.sixpay.bootstrap.integration.e2e;

import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.infrastructure.banking.AmplitudeCustomerVerificationAdapter;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.client.CoreBankingAccessTokenProvider;
import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.error.AmplitudeResponseValidator;
import com.sixpay.customer.verification.infrastructure.banking.error.BankingVerificationErrorClassifier;
import com.sixpay.customer.verification.infrastructure.banking.mapper.AmplitudeCustomerVerificationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "sixpay.e2e.customer",
        name = "enabled",
        havingValue = "true"
)
public class CustomerE2eConfiguration {

    @Bean
    @Primary
    BankingCustomerLookupPort customerE2eBankingCustomerLookupPort() {
        return query ->
                new BankingCustomerLookupPort.BankingCustomerProfile(
                        query.financialInstitutionCode(),
                        "AMPLITUDE-CUSTOMER-CM9",
                        query.customerNumber() == null
                                || query.customerNumber().isBlank()
                                ? "CM9-000001"
                                : query.customerNumber().strip(),
                        query.niu() == null || query.niu().isBlank()
                                ? "CM9-NIU-000001"
                                : query.niu().strip(),
                        "CM9 Full-stack Customer",
                        "cm9.customer@sixpay.test",
                        "+237600000009",
                        new BankingCustomerLookupPort.BankingAccount(
                                query.accountReference(),
                                "v1:" + "a".repeat(64),
                                query.accountReference(),
                                "****4321",
                                "XAF",
                                "CURRENT",
                                Instant.now()
                        )
                );
    }

    @Bean
    @Primary
    BankingCustomerVerificationPort customerE2eBankingVerificationPort(
            @Value("${sixpay.e2e.customer.amplitude-base-url}")
            String amplitudeBaseUrl,
            ObjectMapper objectMapper
    ) {
        BankingVerificationProperties properties =
                new BankingVerificationProperties(
                        URI.create(amplitudeBaseUrl),
                        "/api/v1/customer-verifications",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        1,
                        Duration.ofMillis(50),
                        Duration.ofMinutes(5),
                        new BankingVerificationProperties.Security(
                                "cm9-e2e",
                                "unused"
                        ),
                        new BankingVerificationProperties.Contract(
                                "v1",
                                Set.of("00"),
                                Set.of("01")
                        )
                );

        RestClient restClient =
                RestClient.builder()
                        .baseUrl(amplitudeBaseUrl)
                        .build();

        CoreBankingAccessTokenProvider tokenProvider =
                () -> "cm9-e2e-token";

        AmplitudeCustomerVerificationClient client =
                new AmplitudeCustomerVerificationClient(
                        restClient,
                        tokenProvider,
                        properties,
                        objectMapper
                );

        return new AmplitudeCustomerVerificationAdapter(
                client,
                new AmplitudeCustomerVerificationMapper(
                        properties.evidenceTtl()
                ),
                new AmplitudeResponseValidator(properties),
                new BankingVerificationErrorClassifier()
        );
    }
}
