package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.ObservedCustomerLinkUseCase;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.ObservedCustomerLinkRepository;
import com.sixpay.customer.observation.application.port.input.query.GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.query.GetObservedCustomerQuery;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        prefix = "sixpay.customer.observation.query",
        name = "enabled",
        havingValue = "true"
)
@Transactional
public class ObservedCustomerLinkService
        implements ObservedCustomerLinkUseCase {

    private final GetObservedCustomerUseCase observedCustomerQuery;
    private final CustomerRepository customerRepository;
    private final ObservedCustomerLinkRepository linkRepository;

    public ObservedCustomerLinkService(
            GetObservedCustomerUseCase observedCustomerQuery,
            CustomerRepository customerRepository,
            ObservedCustomerLinkRepository linkRepository
    ) {
        this.observedCustomerQuery =
                Objects.requireNonNull(observedCustomerQuery);
        this.customerRepository =
                Objects.requireNonNull(customerRepository);
        this.linkRepository =
                Objects.requireNonNull(linkRepository);
    }

    @Override
    public ObservedCustomerLink link(
            UUID observedCustomerId,
            CustomerId customerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        requireObservedCustomerExists(observedCustomerId);
        requireMasterCustomerExists(customerId);

        Optional<ObservedCustomerLink> current =
                linkRepository.findByObservedCustomerId(
                        observedCustomerId
                );

        if (current.isEmpty()) {
            return linkRepository.save(
                    ObservedCustomerLink.create(
                            observedCustomerId,
                            customerId,
                            actorId,
                            correlationId,
                            reason,
                            now
                    )
            );
        }

        ObservedCustomerLink existing = current.orElseThrow();

        if (existing.isLinked()) {
            if (existing.customerId().equals(customerId)) {
                return existing;
            }

            throw new CustomerDomainException(
                    "observed customer is already linked "
                            + "to another Customer; unlink first"
            );
        }

        existing.relink(
                customerId,
                actorId,
                correlationId,
                reason,
                now
        );

        return linkRepository.save(existing);
    }

    @Override
    public ObservedCustomerLink unlink(
            UUID observedCustomerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        ObservedCustomerLink link =
                linkRepository.findByObservedCustomerId(
                                observedCustomerId
                        )
                        .orElseThrow(() ->
                                new CustomerDomainException(
                                        "observed customer link not found: "
                                                + observedCustomerId
                                )
                        );

        link.unlink(
                actorId,
                correlationId,
                reason,
                now
        );

        return linkRepository.save(link);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ObservedCustomerLink> findLinked(
            UUID observedCustomerId
    ) {
        return linkRepository.findByObservedCustomerId(
                        observedCustomerId
                )
                .filter(ObservedCustomerLink::isLinked);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservedCustomerLink> findByCustomerId(
            CustomerId customerId
    ) {
        requireMasterCustomerExists(customerId);

        return linkRepository.findLinkedByCustomerId(
                customerId
        );
    }

    private void requireObservedCustomerExists(
            UUID observedCustomerId
    ) {
        observedCustomerQuery.get(
                new GetObservedCustomerQuery(
                        ObservedCustomerId.of(
                                Objects.requireNonNull(
                                        observedCustomerId,
                                        "observedCustomerId is required"
                                )
                        )
                )
        );
    }

    private void requireMasterCustomerExists(
            CustomerId customerId
    ) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerDomainException(
                    "customer not found: " + customerId
            );
        }
    }
}
