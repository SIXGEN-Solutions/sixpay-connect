package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.bootstrap.integration.customer
        .ObservedCustomerProjectionModuleAdapter;
import com.sixpay.bootstrap.integration.customer.mapper
        .PaymentProjectionEventCommandMapper;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.payment.application.port.output
        .ObservedCustomerProjectionPort;
import com.sixpay.payment.application.service
        .PaymentObservedCustomerProjectionRequestFactory;
import com.sixpay.payment.application.service
        .PaymentObservedCustomerProjectionService;
import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxCompletionService;
import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxRepository;
import com.sixpay.payment.infrastructure.outbox.claim
        .PaymentOutboxClaimService;
import com.sixpay.payment.infrastructure.outbox.serialization
        .PaymentOutboxEventDeserializer;
import com.sixpay.payment.infrastructure.outbox.serialization
        .PaymentOutboxEventTypeRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.boot.context.properties
        .EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ObserveCustomerUseCase.class)
@EnableScheduling
@EnableConfigurationProperties(
        CustomerProjectionOutboxProperties.class
)
public class PaymentObservedCustomerOutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PaymentOutboxEventTypeRegistry
            paymentOutboxEventTypeRegistry() {
        return new PaymentOutboxEventTypeRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentOutboxEventDeserializer
            paymentOutboxEventDeserializer(
                    ObjectMapper objectMapper,
                    PaymentOutboxEventTypeRegistry registry
            ) {
        return new PaymentOutboxEventDeserializer(
                objectMapper,
                registry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentProjectionEventCommandMapper
            paymentProjectionEventCommandMapper() {
        return new PaymentProjectionEventCommandMapper();
    }

    @Bean
    @ConditionalOnMissingBean(
            ObservedCustomerProjectionPort.class
    )
    ObservedCustomerProjectionPort
            observedCustomerProjectionPort(
                    ObserveCustomerUseCase useCase,
                    PaymentProjectionEventCommandMapper mapper
            ) {
        return new ObservedCustomerProjectionModuleAdapter(
                useCase,
                mapper
        );
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentObservedCustomerProjectionRequestFactory
            paymentObservedCustomerProjectionRequestFactory() {
        return new PaymentObservedCustomerProjectionRequestFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentObservedCustomerProjectionService
            paymentObservedCustomerProjectionService(
                    ObservedCustomerProjectionPort port,
                    PaymentObservedCustomerProjectionRequestFactory
                            factory
            ) {
        return new PaymentObservedCustomerProjectionService(
                port,
                factory
        );
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentObservedCustomerOutboxHandler
            paymentObservedCustomerOutboxHandler(
                    PaymentObservedCustomerProjectionService service
            ) {
        return new PaymentObservedCustomerOutboxHandler(
                service
        );
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentObservedCustomerOutboxFailureClassifier
            paymentObservedCustomerOutboxFailureClassifier() {
        return new PaymentObservedCustomerOutboxFailureClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentObservedCustomerOutboxDispatcher
            paymentObservedCustomerOutboxDispatcher(
                    PaymentOutboxClaimService claimService,
                    PaymentOutboxEventDeserializer deserializer,
                    PaymentObservedCustomerOutboxHandler handler,
                    PaymentObservedCustomerOutboxFailureClassifier
                            classifier,
                    PaymentOutboxCompletionService completionService
            ) {
        return new PaymentObservedCustomerOutboxDispatcher(
                claimService,
                deserializer,
                handler,
                classifier,
                completionService
        );
    }

    @Bean
    @ConditionalOnMissingBean
    Clock customerProjectionOutboxClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    CustomerProjectionOutboxMetrics
            customerProjectionOutboxMetrics(
                    MeterRegistry meterRegistry
            ) {
        return new CustomerProjectionOutboxMetrics(
                meterRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    CustomerProjectionOutboxHealthIndicator
            customerProjectionOutboxHealthIndicator(
                    PaymentOutboxRepository repository,
                    CustomerProjectionOutboxProperties properties,
                    Clock clock
            ) {
        return new CustomerProjectionOutboxHealthIndicator(
                repository,
                properties,
                clock
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix =
                    "sixpay.payment.outbox.customer-projection",
            name = "enabled",
            havingValue = "true"
    )
    PaymentObservedCustomerOutboxScheduler
            paymentObservedCustomerOutboxScheduler(
                    PaymentObservedCustomerOutboxDispatcher
                            dispatcher,
                    CustomerProjectionOutboxProperties properties,
                    CustomerProjectionOutboxMetrics metrics,
                    PaymentOutboxRepository repository,
                    Clock clock
            ) {
        return new PaymentObservedCustomerOutboxScheduler(
                dispatcher,
                properties,
                metrics,
                repository,
                clock
        );
    }
}
