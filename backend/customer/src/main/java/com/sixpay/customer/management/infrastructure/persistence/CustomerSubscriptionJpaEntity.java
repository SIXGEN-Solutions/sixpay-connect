package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_management_subscription")
public class CustomerSubscriptionJpaEntity {

    @Id
    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CustomerSubscriptionStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected CustomerSubscriptionJpaEntity() {
    }

    static CustomerSubscriptionJpaEntity create(
            CustomerSubscription subscription
    ) {
        CustomerSubscriptionJpaEntity entity =
                new CustomerSubscriptionJpaEntity();

        entity.id = subscription.id().value();
        entity.customerId =
                subscription.customerId().value();
        entity.partnerId = subscription.partnerId();
        entity.bankAccountId =
                subscription.bankAccountId().value();
        entity.createdAt = subscription.createdAt();
        entity.synchronize(subscription);

        return entity;
    }

    void synchronize(CustomerSubscription subscription) {
        bankAccountId =
                subscription.bankAccountId().value();
        status = subscription.status();
        statusReason =
                subscription.statusReason().orElse(null);
        activatedAt =
                subscription.activatedAt().orElse(null);
        updatedAt = subscription.updatedAt();
        closedAt =
                subscription.closedAt().orElse(null);
    }

    UUID id() {
        return id;
    }

    UUID customerId() {
        return customerId;
    }

    UUID partnerId() {
        return partnerId;
    }

    UUID bankAccountId() {
        return bankAccountId;
    }

    CustomerSubscriptionStatus status() {
        return status;
    }

    String statusReason() {
        return statusReason;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant activatedAt() {
        return activatedAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    Instant closedAt() {
        return closedAt;
    }
}
