package com.sixpay.customer.verification.infrastructure.banking.configuration;

import com.sixpay.customer.management.infrastructure.banking.amplitude.AmplitudeCustomerLookupClient;
import com.sixpay.customer.verification.infrastructure.banking.client.AmplitudeCustomerVerificationClient;
import com.sixpay.customer.verification.infrastructure.banking.client.CoreBankingAccessTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** CB-3: protect the existing shared Customer banking HTTP, OAuth2 and mTLS composition. */
class CustomerBankingSharedTransportWiringTest {

    @Test
    void verificationAndLookupDependOnSameTransportAndTechnicalTokenProvider() throws Exception {
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
        assertThat(restClient.getParameterTypes()).contains(
                com.sixpay.integration.http.StandardRestClientFactory.class,
                BankingVerificationProperties.class,
                org.springframework.boot.ssl.SslBundles.class);
        assertThat(lookup.isAnnotationPresent(Bean.class)).isTrue();
        assertThat(verification.isAnnotationPresent(Bean.class)).isTrue();
        assertThat(lookup.getReturnType()).isEqualTo(AmplitudeCustomerLookupClient.class);
        assertThat(verification.getReturnType()).isEqualTo(AmplitudeCustomerVerificationClient.class);
        assertThat(lookup.getParameterTypes()).contains(RestClient.class, CoreBankingAccessTokenProvider.class);
        assertThat(verification.getParameterTypes()).contains(RestClient.class, CoreBankingAccessTokenProvider.class);
    }

    @Test
    void onlyOneTechnicalTokenProviderBeanIsDeclaredInBankingConfiguration() {
        long providers = Arrays.stream(AmplitudeCustomerVerificationConfiguration.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class))
                .filter(method -> method.getReturnType().equals(CoreBankingAccessTokenProvider.class))
                .count();
        assertThat(providers).isEqualTo(1);
    }
}
