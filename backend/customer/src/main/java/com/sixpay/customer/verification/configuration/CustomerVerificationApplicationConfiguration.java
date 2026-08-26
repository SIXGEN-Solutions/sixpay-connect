package com.sixpay.customer.verification.configuration;

import com.sixpay.customer.configuration.CustomerCapabilityUnavailableException;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationDomainEventPublisher;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationEventIdGenerator;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationRepository;
import com.sixpay.customer.verification.application.port.output.CustomerVerificationTimeProvider;
import com.sixpay.customer.verification.application.service.CustomerVerificationService;
import com.sixpay.customer.verification.domain.event.CustomerVerificationDomainEvent;
import com.sixpay.customer.verification.domain.model.CustomerVerification;
import com.sixpay.customer.verification.domain.model.CustomerVerificationId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Environment-neutral application wiring for Customer Verification.
 *
 * <p>The business use case is identical in every environment. Only output
 * port implementations may vary.</p>
 *
 * <p>Missing critical infrastructure fails closed at invocation time.
 * No profile is allowed to manufacture successful banking evidence.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CustomerVerificationApplicationConfiguration {

    @Bean
    VerifyCustomerUseCase verifyCustomerUseCase(
            BankingCustomerVerificationPort bankingPort,
            CustomerVerificationRepository repository,
            CustomerVerificationDomainEventPublisher eventPublisher,
            CustomerVerificationEventIdGenerator eventIdGenerator,
            CustomerVerificationTimeProvider timeProvider
    ) {
        return new CustomerVerificationService(
                bankingPort,
                repository,
                eventPublisher,
                eventIdGenerator,
                timeProvider
        );
    }

    @Bean
    @ConditionalOnMissingBean(CustomerVerificationTimeProvider.class)
    CustomerVerificationTimeProvider customerVerificationTimeProvider() {
        return Instant::now;
    }

    @Bean
    @ConditionalOnMissingBean(CustomerVerificationEventIdGenerator.class)
    CustomerVerificationEventIdGenerator
            customerVerificationEventIdGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    @ConditionalOnMissingBean(CustomerVerificationDomainEventPublisher.class)
    CustomerVerificationDomainEventPublisher
            customerVerificationDomainEventPublisher(
                    ApplicationEventPublisher publisher
            ) {
        return events -> publishAll(publisher, events);
    }

    /**
     * Fail-closed persistence boundary.
     *
     * <p>The authoritative branch defines the repository port but does not
     * yet contain a runtime persistence adapter for Customer Verification.
     * Refusing writes is safer than silently using volatile memory.</p>
     */
    @Bean
    @ConditionalOnMissingBean(CustomerVerificationRepository.class)
    CustomerVerificationRepository
            unavailableCustomerVerificationRepository() {

        return new CustomerVerificationRepository() {
            @Override
            public CustomerVerification save(
                    CustomerVerification verification
            ) {
                throw unavailable(
                        "Customer Verification persistence is not configured"
                );
            }

            @Override
            public Optional<CustomerVerification> findById(
                    CustomerVerificationId verificationId
            ) {
                throw unavailable(
                        "Customer Verification persistence is not configured"
                );
            }
        };
    }

    /**
     * Fail-closed banking verification boundary.
     *
     * <p>When the approved Amplitude configuration is enabled, its concrete
     * BankingCustomerVerificationPort replaces this fallback.</p>
     */
    @Bean
    @ConditionalOnMissingBean(BankingCustomerVerificationPort.class)
    BankingCustomerVerificationPort
            unavailableBankingCustomerVerificationPort() {

        return new BankingCustomerVerificationPort() {
            @Override
            public BankingVerificationResponse verify(
                    BankingVerificationQuery query
            ) {
                throw unavailable(
                        "Customer banking verification is not configured"
                );
            }
        };
    }

    /**
     * Fail-closed Customer lookup boundary.
     *
     * <p>No bank-approved runtime implementation of the Customer lookup
     * contract exists on the authoritative branch yet.</p>
     */
    @Bean
    @ConditionalOnMissingBean(BankingCustomerLookupPort.class)
    BankingCustomerLookupPort unavailableBankingCustomerLookupPort() {
        return query -> {
            throw unavailable(
                    "Customer banking lookup is not configured"
            );
        };
    }

    private static void publishAll(
            ApplicationEventPublisher publisher,
            List<CustomerVerificationDomainEvent> events
    ) {
        events.forEach(publisher::publishEvent);
    }

    private static CustomerCapabilityUnavailableException unavailable(
            String message
    ) {
        return new CustomerCapabilityUnavailableException(message);
    }
}
