package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
import com.sixpay.customer.management.domain.repository.ObservedCustomerLinkRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ObservedCustomerLinkRepositoryAdapter
        implements ObservedCustomerLinkRepository {

    private final ObservedCustomerLinkSpringDataRepository repository;

    public ObservedCustomerLinkRepositoryAdapter(
            ObservedCustomerLinkSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public ObservedCustomerLink save(
            ObservedCustomerLink link
    ) {
        ObservedCustomerLinkJpaEntity entity =
                repository.findById(
                                link.observedCustomerId()
                        )
                        .orElseGet(() ->
                                ObservedCustomerLinkJpaEntity
                                        .create(link)
                        );

        entity.synchronize(link);
        repository.save(entity);

        return link;
    }

    @Override
    public Optional<ObservedCustomerLink>
            findByObservedCustomerId(
                    UUID observedCustomerId
            ) {
        return repository.findById(observedCustomerId)
                .map(this::toDomain);
    }

    @Override
    public List<ObservedCustomerLink> findLinkedByCustomerId(
            CustomerId customerId
    ) {
        return repository
                .findByCustomerIdAndStatusOrderByLinkedAtDesc(
                        customerId.value(),
                        ObservedCustomerLinkStatus.LINKED
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ObservedCustomerLink toDomain(
            ObservedCustomerLinkJpaEntity entity
    ) {
        return ObservedCustomerLink.reconstitute(
                entity.observedCustomerId(),
                new CustomerId(entity.customerId()),
                entity.status(),
                entity.linkedBy(),
                entity.linkCorrelationId(),
                entity.linkReason(),
                entity.linkedAt(),
                entity.unlinkedBy(),
                entity.unlinkCorrelationId(),
                entity.unlinkReason(),
                entity.unlinkedAt()
        );
    }
}
