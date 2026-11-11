package com.sixpay.customer.verification.infrastructure.banking.configuration;

import com.sixpay.customer.management.infrastructure.banking.amplitude.AmplitudeCustomerLookupClient;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** CB-3: protect the existing shared Customer banking HTTP composition without technical OAuth2 or mTLS. */
class CustomerBankingSharedTransportWiringTest {

    @Test
    void verificationAndLookupDependOnSameTransportWithoutTechnicalTokenProvider() throws Exception {
        Class<?> configuration = AmplitudeCustomerVerificationConfiguration.class;
        Method restClient = Arrays.stream(configuration.getDeclaredMethods())
                .filter(method -> method.getName().equals("amplitudeCustomerVerificationRestClient"))
                .findFirst().orElseThrow();
        Method lookup = Arrays.stream(configuration.getDeclaredMethods())
                .filter(method -> method.getName().equals("amplitudeCustomerLookupClient"))
                .findFirst().orElseThrow();
        Method verification = Arrays.stream(configuration.getDeclaredMethods())
                .filter(method -> method.getName().equals("amplitudeCustomerVerificationClient"))
                .findFirst().orElseThrow();

        assertThat(restClient.isAnnotationPresent(Bean.class)).isTrue();
        assertThat(restClient.getReturnType()).isEqualTo(RestClient.class);
        assertThat(restClient.getParameterTypes()).containsExactly(
                com.sixpay.integration.http.StandardRestClientFactory.class,
                BankingVerificationProperties.class);
        assertThat(lookup.isAnnotationPresent(Bean.class)).isTrue();
        assertThat(verification.isAnnotationPresent(Bean.class)).isTrue();
        assertThat(lookup.getReturnType()).isEqualTo(AmplitudeCustomerLookupClient.class);
        assertThat(verification.getReturnType()).isEqualTo(AmplitudeCustomerVerificationClient.class);
        assertThat(lookup.getParameterTypes()).containsExactly(RestClient.class);
        assertThat(verification.getParameterTypes()).containsExactly(RestClient.class, BankingVerificationProperties.class, tools.jackson.databind.ObjectMapper.class);
    }


}
