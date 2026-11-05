package com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePaymentEventAdapter;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePaymentEventRecoveryAdapter;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.*;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper.AmplitudePaymentEventMapper;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.configuration.PaymentConfigurationParameterPort;
import com.sixpay.payment.application.service.PaymentEventLifecycleOrchestrationService;
import com.sixpay.payment.application.service.PaymentPostingPreparationService;
import com.sixpay.payment.application.service.PaymentT0PostingReservationService;
import com.sixpay.payment.application.service.PaymentFinalizationService;
import com.sixpay.payment.application.service.PaymentEventContextService;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort;
import org.springframework.beans.factory.ObjectProvider;
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
        AmplitudePostingProperties.class
)
@ConditionalOnProperty(
        prefix = AmplitudePostingProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudePostingConfiguration {

    @Bean
    PostingAccessTokenProvider postingAccessTokenProvider(
            ObjectProvider<OAuth2AuthorizedClientManager> managerProvider,
            AmplitudePostingProperties properties
    ) {
        if (!properties.security().oauth2Enabled()) {
            return () -> null;
        }
        OAuth2AuthorizedClientManager manager = managerProvider.getIfAvailable();
        if (manager == null) {
            throw new IllegalStateException(
                    "OAuth2 manager required when posting OAuth2 is enabled");
        }
        return new OAuth2PostingAccessTokenProvider(manager, properties);
    }

    @Bean
    RestClient amplitudePostingRestClient(
            StandardRestClientFactory factory,
            AmplitudePostingProperties properties,
            SslBundles sslBundles
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(
                        properties.connectTimeout(),
                        properties.readTimeout()
                ),
                properties.security().mtlsEnabled()
                        ? sslBundles.getBundle(properties.security().sslBundle())
                                .createSslContext()
                        : null,
                List.of(),
                properties.security().allowInsecureHttp()
        );
    }



    @Bean
    AmplitudePaymentEventRecoveryClient amplitudePaymentEventRecoveryClient(
            RestClient amplitudePostingRestClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        return new RestAmplitudePaymentEventRecoveryClient(
                amplitudePostingRestClient,
                tokenProvider,
                properties
        );
    }

    @Bean
    PaymentEventRecoveryPort paymentEventRecoveryPort(
            AmplitudePaymentEventRecoveryClient client
    ) {
        return new AmplitudePaymentEventRecoveryAdapter(client);
    }

    @Bean
    AmplitudePaymentEventMapper amplitudePaymentEventMapper() {
        return new AmplitudePaymentEventMapper();
    }

    @Bean
    AmplitudePaymentEventClient amplitudePaymentEventClient(
            RestClient amplitudePostingRestClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        return new RestAmplitudePaymentEventClient(
                amplitudePostingRestClient,
                tokenProvider,
                properties
        );
    }

    @Bean
    PaymentEventExecutionPort paymentEventExecutionPort(
            AmplitudePaymentEventClient client,
            AmplitudePaymentEventMapper mapper
    ) {
        return new AmplitudePaymentEventAdapter(client, mapper);
    }

    // The Posting capability gates this service. Declare the bean here rather
    // than applying @ConditionalOnBean to a component-scanned @Service: the
    // latter may be evaluated before the execution/recovery ports are registered.
    // Register application orchestration explicitly with the Posting capability.
    // Avoid @ConditionalOnBean on component-scanned services: port definitions
    // may not yet be registered when component conditions are evaluated.
    @Bean
    PaymentEventContextService paymentEventContextService(
            PaymentConfigurationParameterPort parameterPort
    ) {
        return new PaymentEventContextService(parameterPort);
    }

    @Bean
    PaymentEventLifecycleOrchestrationService paymentEventLifecycleOrchestrationService(
            PaymentLookupPort paymentLookupPort,
            PaymentFinancialSnapshotRepository snapshotRepository,
            PaymentPostingPreparationService postingPreparationService,
            PaymentT0PostingReservationService reservationService,
            PaymentFinalizationService finalizationService,
            PaymentEventContextService contextService,
            PaymentEventExecutionPort executionPort,
            PaymentEventRecoveryPort recoveryPort
    ) {
        return new PaymentEventLifecycleOrchestrationService(
                paymentLookupPort,
                snapshotRepository,
                postingPreparationService,
                reservationService,
                finalizationService,
                contextService,
                executionPort,
                recoveryPort
        );
    }

}
