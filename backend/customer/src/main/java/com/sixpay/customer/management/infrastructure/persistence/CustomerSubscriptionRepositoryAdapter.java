package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionStatus;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomerSubscriptionRepositoryAdapter
        implements CustomerSubscriptionRepository {

    private static final EnumSet<CustomerSubscriptionStatus>
            OPEN_STATUSES =
            EnumSet.of(
                    CustomerSubscriptionStatus.PENDING_ACTIVATION,
                    CustomerSubscriptionStatus.ACTIVE,
                    CustomerSubscriptionStatus.SUSPENDED
            );

    private final CustomerSubscriptionSpringDataRepository repository;

    public CustomerSubscriptionRepositoryAdapter(
            CustomerSubscriptionSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public CustomerSubscription save(
            CustomerSubscription subscription
    ) {
        CustomerSubscriptionJpaEntity entity =
                repository.findById(
                                subscription.id().value()
                        )
                        .orElseGet(() ->
                                CustomerSubscriptionJpaEntity
                                        .create(subscription)
                        );

        entity.synchronize(subscription);
        repository.save(entity);

        return subscription;
    }

    @Override
    public Optional<CustomerSubscription> findById(
            CustomerSubscriptionId subscriptionId
    ) {
        return repository.findById(subscriptionId.value())
                .map(this::toDomain);
    }

    @Override
    public List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    ) {
        return repository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId.value()
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsOpenByCustomerIdAndPartnerId(
            CustomerId customerId,
            UUID partnerId
    ) {
        return repository
                .existsByCustomerIdAndPartnerIdAndStatusIn(
                        customerId.value(),
                        partnerId,
                        OPEN_STATUSES
                );
    }

    private CustomerSubscription toDomain(
            CustomerSubscriptionJpaEntity entity
    ) {
        return CustomerSubscription.reconstitute(
                new CustomerSubscriptionId(
                        entity.id()
                ),
                new CustomerId(
                        entity.customerId()
                ),
                entity.partnerId(),
                new CustomerBankAccountId(
                        entity.bankAccountId()
                ),
                entity.status(),
                entity.statusReason(),
                entity.createdAt(),
                entity.activatedAt(),
                entity.updatedAt(),
                entity.closedAt()
        );
    }
}
