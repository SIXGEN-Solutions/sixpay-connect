package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.bootstrap.integration.customer.ObservedCustomerProjectionModuleAdapter;
import com.sixpay.bootstrap.integration.customer.mapper.PaymentProjectionEventCommandMapper;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.service.PaymentObservedCustomerProjectionRequestFactory;
import com.sixpay.payment.application.service.PaymentObservedCustomerProjectionService;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxCompletionService;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxEventDeserializer;
import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxEventTypeRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Explicit composition of the Payment-to-Customer outbox projection chain.
 * Scheduling is intentionally deferred to lot 4.6.5.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ObserveCustomerUseCase.class)
public class PaymentObservedCustomerOutboxConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PaymentOutboxEventTypeRegistry paymentOutboxEventTypeRegistry() {
        return new PaymentOutboxEventTypeRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    PaymentOutboxEventDeserializer paymentOutboxEventDeserializer(
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
    PaymentProjectionEventCommandMapper paymentProjectionEventCommandMapper() {
        return new PaymentProjectionEventCommandMapper();
    }

    @Bean
    @ConditionalOnMissingBean(ObservedCustomerProjectionPort.class)
    ObservedCustomerProjectionPort observedCustomerProjectionPort(
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
                    PaymentObservedCustomerProjectionRequestFactory factory
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
        return new PaymentObservedCustomerOutboxHandler(service);
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
                    PaymentObservedCustomerOutboxFailureClassifier classifier,
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
}
