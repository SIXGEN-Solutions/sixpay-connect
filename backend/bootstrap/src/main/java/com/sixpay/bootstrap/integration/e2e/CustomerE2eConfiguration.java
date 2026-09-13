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
import java.util.HexFormat;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
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
                        "AMPLITUDE-CUSTOMER-" + normalizedNiu(query.niu()),
                        query.customerNumber() == null
                                || query.customerNumber().isBlank()
                                ? "CM9-000001"
                                : query.customerNumber().strip(),
                        normalizedNiu(query.niu()),
                        "CM9 Full-stack Customer",
                        "cm9.customer@sixpay.test",
                        "+237600000009",
                        new BankingCustomerLookupPort.BankingAccount(
                                query.accountReference(),
                                accountBindingFingerprint(
                                        query.accountReference()
                                ),
                                query.accountReference(),
                                "****4321",
                                "XAF",
                                "CURRENT",
                                Instant.now()
                        )
                );
    }

    private static String accountBindingFingerprint(
            String accountReference
    ) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(
                            accountReference.strip()
                                    .getBytes(StandardCharsets.UTF_8)
                    );
            return "v1:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private static String normalizedNiu(String niu) {
        return niu == null || niu.isBlank()
                ? "CM9-NIU-000001"
                : niu.strip();
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
