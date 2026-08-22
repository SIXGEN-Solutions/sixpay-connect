#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
BRANCH = "feat/sixpay-customer-management-baseline"

def run(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True)

def guard():
    r = run("git", "rev-parse", "--show-toplevel")
    if r.returncode != 0:
        raise SystemExit("Run inside sixpay-connect.")
    if Path(r.stdout.strip()).resolve() != ROOT.resolve():
        raise SystemExit("Run from repository root.")
    b = run("git", "branch", "--show-current").stdout.strip()
    if b != BRANCH:
        raise SystemExit(f"Expected branch {BRANCH}, got {b}")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        current = p.read_text(encoding="utf-8")
        if current == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(f"[stop] {rel} already exists with different content")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

guard()

BASE = "backend/customer/src/main/java/com/sixpay/customer/management"
MODEL = BASE + "/domain/model"
REPO = BASE + "/domain/repository"
IN = BASE + "/application/port/input"
OUT = BASE + "/application/port/output"
SVC = BASE + "/application/service"
PERSIST = BASE + "/infrastructure/persistence"
API = BASE + "/api"
REQ = API + "/request"
RESP = API + "/response"
MIG = "backend/customer/src/main/resources/db/migration"
TEST = "backend/customer/src/test/java/com/sixpay/customer/management/subscription"
BOOT = "backend/bootstrap/src/main/java/com/sixpay/bootstrap/integration/customer"

create(MODEL + "/CustomerSubscriptionId.java", """package com.sixpay.customer.management.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerSubscriptionId(UUID value) {

    public CustomerSubscriptionId {
        Objects.requireNonNull(value, "value is required");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
""")

create(MODEL + "/CustomerSubscriptionStatus.java", """package com.sixpay.customer.management.domain.model;

public enum CustomerSubscriptionStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    SUSPENDED,
    CLOSED
}
""")

create(MODEL + "/CustomerSubscription.java", """package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.sharedkernel.domain.model.AggregateRoot;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CustomerSubscription
        extends AggregateRoot<CustomerSubscriptionId> {

    private static final int MAX_REASON_LENGTH = 500;

    private final CustomerId customerId;
    private final UUID partnerId;
    private final CustomerBankAccountId bankAccountId;
    private final Instant createdAt;

    private CustomerSubscriptionStatus status;
    private String statusReason;
    private Instant activatedAt;
    private Instant updatedAt;
    private Instant closedAt;

    private CustomerSubscription(
            CustomerSubscriptionId id,
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            CustomerSubscriptionStatus status,
            String statusReason,
            Instant createdAt,
            Instant activatedAt,
            Instant updatedAt,
            Instant closedAt
    ) {
        super(id);
        this.customerId = Objects.requireNonNull(
                customerId,
                "customerId is required"
        );
        this.partnerId = Objects.requireNonNull(
                partnerId,
                "partnerId is required"
        );
        this.bankAccountId = Objects.requireNonNull(
                bankAccountId,
                "bankAccountId is required"
        );
        this.status = Objects.requireNonNull(
                status,
                "status is required"
        );
        this.statusReason = normalizeReason(statusReason);
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt is required"
        );
        this.activatedAt = activatedAt;
        this.updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );
        this.closedAt = closedAt;

        validateTimeline();
        validateStatusState();
    }

    public static CustomerSubscription create(
            CustomerSubscriptionId id,
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now is required");

        return new CustomerSubscription(
                id,
                customerId,
                partnerId,
                bankAccountId,
                CustomerSubscriptionStatus.PENDING_ACTIVATION,
                null,
                now,
                null,
                now,
                null
        );
    }

    public static CustomerSubscription reconstitute(
            CustomerSubscriptionId id,
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            CustomerSubscriptionStatus status,
            String statusReason,
            Instant createdAt,
            Instant activatedAt,
            Instant updatedAt,
            Instant closedAt
    ) {
        return new CustomerSubscription(
                id,
                customerId,
                partnerId,
                bankAccountId,
                status,
                statusReason,
                createdAt,
                activatedAt,
                updatedAt,
                closedAt
        );
    }

    public void activate(Instant now) {
        requireTime(now);

        if (status != CustomerSubscriptionStatus.PENDING_ACTIVATION
                && status != CustomerSubscriptionStatus.SUSPENDED) {
            throw new CustomerDomainException(
                    "cannot activate subscription in status " + status
            );
        }

        status = CustomerSubscriptionStatus.ACTIVE;
        statusReason = null;

        if (activatedAt == null) {
            activatedAt = now;
        }

        updatedAt = now;
    }

    public void suspend(String reason, Instant now) {
        requireStatus(
                CustomerSubscriptionStatus.ACTIVE,
                "suspend"
        );
        requireTime(now);

        status = CustomerSubscriptionStatus.SUSPENDED;
        statusReason = requireReason(reason);
        updatedAt = now;
    }

    public void close(String reason, Instant now) {
        if (status == CustomerSubscriptionStatus.CLOSED) {
            throw new CustomerDomainException(
                    "cannot close subscription already in status CLOSED"
            );
        }

        requireTime(now);

        status = CustomerSubscriptionStatus.CLOSED;
        statusReason = requireReason(reason);
        updatedAt = now;
        closedAt = now;
    }

    public boolean acceptsPayments() {
        return status == CustomerSubscriptionStatus.ACTIVE;
    }

    private void requireStatus(
            CustomerSubscriptionStatus expected,
            String operation
    ) {
        if (status != expected) {
            throw new CustomerDomainException(
                    "cannot "
                            + operation
                            + " subscription in status "
                            + status
                            + "; expected "
                            + expected
            );
        }
    }

    private void requireTime(Instant now) {
        Objects.requireNonNull(now, "now is required");

        if (now.isBefore(updatedAt)) {
            throw new CustomerDomainException(
                    "operation time must not precede updatedAt"
            );
        }
    }

    private void validateTimeline() {
        if (updatedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "updatedAt must not precede createdAt"
            );
        }

        if (activatedAt != null
                && activatedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "activatedAt must not precede createdAt"
            );
        }

        if (closedAt != null
                && closedAt.isBefore(createdAt)) {
            throw new CustomerDomainException(
                    "closedAt must not precede createdAt"
            );
        }
    }

    private void validateStatusState() {
        if (status == CustomerSubscriptionStatus.PENDING_ACTIVATION
                && (statusReason != null
                || activatedAt != null
                || closedAt != null)) {
            throw new CustomerDomainException(
                    "pending subscription has invalid lifecycle state"
            );
        }

        if (status == CustomerSubscriptionStatus.ACTIVE
                && statusReason != null) {
            throw new CustomerDomainException(
                    "ACTIVE subscription must not have a status reason"
            );
        }

        if (status == CustomerSubscriptionStatus.SUSPENDED
                && statusReason == null) {
            throw new CustomerDomainException(
                    "SUSPENDED subscription requires a reason"
            );
        }

        if (status == CustomerSubscriptionStatus.CLOSED
                && (statusReason == null || closedAt == null)) {
            throw new CustomerDomainException(
                    "CLOSED subscription requires reason and closedAt"
            );
        }
    }

    private static String requireReason(String value) {
        String normalized = normalizeReason(value);
        if (normalized == null) {
            throw new CustomerDomainException(
                    "a reason is required"
            );
        }
        return normalized;
    }

    private static String normalizeReason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new CustomerDomainException(
                    "reason must not exceed "
                            + MAX_REASON_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public UUID partnerId() {
        return partnerId;
    }

    public CustomerBankAccountId bankAccountId() {
        return bankAccountId;
    }

    public CustomerSubscriptionStatus status() {
        return status;
    }

    public Optional<String> statusReason() {
        return Optional.ofNullable(statusReason);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> activatedAt() {
        return Optional.ofNullable(activatedAt);
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}
""")

create(REPO + "/CustomerSubscriptionRepository.java", """package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerSubscriptionRepository {

    CustomerSubscription save(CustomerSubscription subscription);

    Optional<CustomerSubscription> findById(
            CustomerSubscriptionId subscriptionId
    );

    List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    );

    boolean existsOpenByCustomerIdAndPartnerId(
            CustomerId customerId,
            UUID partnerId
    );
}
""")

create(OUT + "/PartnerSubscriptionEligibilityPort.java", """package com.sixpay.customer.management.application.port.output;

import java.util.UUID;

public interface PartnerSubscriptionEligibilityPort {

    PartnerEligibility check(UUID partnerId);

    record PartnerEligibility(
            boolean exists,
            boolean active
    ) {
    }
}
""")

create(IN + "/CustomerSubscriptionUseCase.java", """package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerSubscriptionUseCase {

    CustomerSubscription create(
            CustomerId customerId,
            UUID partnerId,
            com.sixpay.customer.management.domain.model.CustomerBankAccountId bankAccountId,
            Instant now
    );

    CustomerSubscription activate(
            CustomerSubscriptionId subscriptionId,
            Instant now
    );

    CustomerSubscription suspend(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    );

    CustomerSubscription close(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    );

    CustomerSubscription findById(
            CustomerSubscriptionId subscriptionId
    );

    List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    );
}
""")

create(SVC + "/CustomerSubscriptionService.java", """package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.CustomerSubscriptionUseCase;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.PartnerSubscriptionEligibilityPort;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public final class CustomerSubscriptionService
        implements CustomerSubscriptionUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerSubscriptionRepository subscriptionRepository;
    private final PartnerSubscriptionEligibilityPort partnerEligibility;
    private final CustomerEnrollmentIdGenerator idGenerator;

    public CustomerSubscriptionService(
            CustomerRepository customerRepository,
            CustomerSubscriptionRepository subscriptionRepository,
            PartnerSubscriptionEligibilityPort partnerEligibility,
            CustomerEnrollmentIdGenerator idGenerator
    ) {
        this.customerRepository =
                Objects.requireNonNull(customerRepository);
        this.subscriptionRepository =
                Objects.requireNonNull(subscriptionRepository);
        this.partnerEligibility =
                Objects.requireNonNull(partnerEligibility);
        this.idGenerator =
                Objects.requireNonNull(idGenerator);
    }

    @Override
    public CustomerSubscription create(
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            Instant now
    ) {
        Customer customer = loadCustomer(customerId);

        requireCustomerEligible(customer);
        requireAccountBelongsToCustomer(customer, bankAccountId);
        requirePartnerActive(partnerId);

        if (subscriptionRepository
                .existsOpenByCustomerIdAndPartnerId(
                        customerId,
                        partnerId
                )) {
            throw new CustomerDomainException(
                    "an open subscription already exists "
                            + "for customer and partner"
            );
        }

        CustomerSubscription subscription =
                CustomerSubscription.create(
                        new CustomerSubscriptionId(
                                idGenerator.nextId()
                        ),
                        customerId,
                        partnerId,
                        bankAccountId,
                        now
                );

        return subscriptionRepository.save(subscription);
    }

    @Override
    public CustomerSubscription activate(
            CustomerSubscriptionId subscriptionId,
            Instant now
    ) {
        CustomerSubscription subscription =
                loadSubscription(subscriptionId);

        Customer customer =
                loadCustomer(subscription.customerId());

        requireCustomerEligible(customer);
        requireAccountBelongsToCustomer(
                customer,
                subscription.bankAccountId()
        );
        requirePartnerActive(subscription.partnerId());

        subscription.activate(now);

        return subscriptionRepository.save(subscription);
    }

    @Override
    public CustomerSubscription suspend(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    ) {
        CustomerSubscription subscription =
                loadSubscription(subscriptionId);

        subscription.suspend(reason, now);

        return subscriptionRepository.save(subscription);
    }

    @Override
    public CustomerSubscription close(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    ) {
        CustomerSubscription subscription =
                loadSubscription(subscriptionId);

        subscription.close(reason, now);

        return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSubscription findById(
            CustomerSubscriptionId subscriptionId
    ) {
        return loadSubscription(subscriptionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    ) {
        return subscriptionRepository.findByCustomerId(
                customerId
        );
    }

    private Customer loadCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new CustomerDomainException(
                                "customer not found: "
                                        + customerId
                        )
                );
    }

    private CustomerSubscription loadSubscription(
            CustomerSubscriptionId subscriptionId
    ) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() ->
                        new CustomerDomainException(
                                "subscription not found: "
                                        + subscriptionId
                        )
                );
    }

    private static void requireCustomerEligible(
            Customer customer
    ) {
        if (!customer.acceptsNewSubscriptions()) {
            throw new CustomerDomainException(
                    "customer is not eligible for subscription"
            );
        }
    }

    private static void requireAccountBelongsToCustomer(
            Customer customer,
            CustomerBankAccountId accountId
    ) {
        boolean belongs =
                customer.bankAccounts()
                        .stream()
                        .anyMatch(account ->
                                account.id()
                                        .equals(accountId)
                        );

        if (!belongs) {
            throw new CustomerDomainException(
                    "subscription account does not belong to customer"
            );
        }
    }

    private void requirePartnerActive(UUID partnerId) {
        var eligibility = partnerEligibility.check(partnerId);

        if (!eligibility.exists()) {
            throw new CustomerDomainException(
                    "partner not found: " + partnerId
            );
        }

        if (!eligibility.active()) {
            throw new CustomerDomainException(
                    "partner is not active: " + partnerId
            );
        }
    }
}
""")

create(PERSIST + "/CustomerSubscriptionJpaEntity.java", """package com.sixpay.customer.management.infrastructure.persistence;

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
""")

create(PERSIST + "/CustomerSubscriptionSpringDataRepository.java", """package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CustomerSubscriptionSpringDataRepository
        extends JpaRepository<CustomerSubscriptionJpaEntity, UUID> {

    List<CustomerSubscriptionJpaEntity> findByCustomerIdOrderByCreatedAtDesc(
            UUID customerId
    );

    boolean existsByCustomerIdAndPartnerIdAndStatusIn(
            UUID customerId,
            UUID partnerId,
            Collection<CustomerSubscriptionStatus> statuses
    );
}
""")

create(PERSIST + "/CustomerSubscriptionRepositoryAdapter.java", """package com.sixpay.customer.management.infrastructure.persistence;

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
""")

create(MIG + "/V20260822.02__create_customer_subscription.sql", """CREATE TABLE customer_management_subscription (
    subscription_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    partner_id UUID NOT NULL,
    bank_account_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_customer_subscription_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer_management_customer (customer_id),

    CONSTRAINT fk_customer_subscription_account
        FOREIGN KEY (bank_account_id)
        REFERENCES customer_management_bank_account (bank_account_id),

    CONSTRAINT ck_customer_subscription_status
        CHECK (
            status IN (
                'PENDING_ACTIVATION',
                'ACTIVE',
                'SUSPENDED',
                'CLOSED'
            )
        ),

    CONSTRAINT ck_customer_subscription_timeline
        CHECK (
            updated_at >= created_at
            AND (
                activated_at IS NULL
                OR activated_at >= created_at
            )
            AND (
                closed_at IS NULL
                OR closed_at >= created_at
            )
        ),

    CONSTRAINT ck_customer_subscription_state
        CHECK (
            (
                status = 'PENDING_ACTIVATION'
                AND status_reason IS NULL
                AND activated_at IS NULL
                AND closed_at IS NULL
            )
            OR (
                status = 'ACTIVE'
                AND status_reason IS NULL
                AND activated_at IS NOT NULL
                AND closed_at IS NULL
            )
            OR (
                status = 'SUSPENDED'
                AND status_reason IS NOT NULL
                AND activated_at IS NOT NULL
                AND closed_at IS NULL
            )
            OR (
                status = 'CLOSED'
                AND status_reason IS NOT NULL
                AND closed_at IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uk_customer_subscription_open_partner
    ON customer_management_subscription (
        customer_id,
        partner_id
    )
    WHERE status <> 'CLOSED';

CREATE INDEX ix_customer_subscription_customer
    ON customer_management_subscription (
        customer_id,
        created_at DESC
    );

CREATE INDEX ix_customer_subscription_partner
    ON customer_management_subscription (
        partner_id,
        status
    );

CREATE INDEX ix_customer_subscription_account
    ON customer_management_subscription (
        bank_account_id
    );
""")

create(REQ + "/CreateCustomerSubscriptionRequest.java", """package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCustomerSubscriptionRequest(
        @NotNull UUID customerId,
        @NotNull UUID partnerId,
        @NotNull UUID bankAccountId
) {
}
""")

create(REQ + "/SubscriptionReasonRequest.java", """package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubscriptionReasonRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {
}
""")

create(RESP + "/CustomerSubscriptionResponse.java", """package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.CustomerSubscription;

import java.time.Instant;
import java.util.UUID;

public record CustomerSubscriptionResponse(
        UUID id,
        UUID customerId,
        UUID partnerId,
        UUID bankAccountId,
        String status,
        String statusReason,
        Instant createdAt,
        Instant activatedAt,
        Instant updatedAt,
        Instant closedAt
) {
    public static CustomerSubscriptionResponse from(
            CustomerSubscription subscription
    ) {
        return new CustomerSubscriptionResponse(
                subscription.id().value(),
                subscription.customerId().value(),
                subscription.partnerId(),
                subscription.bankAccountId().value(),
                subscription.status().name(),
                subscription.statusReason().orElse(null),
                subscription.createdAt(),
                subscription.activatedAt().orElse(null),
                subscription.updatedAt(),
                subscription.closedAt().orElse(null)
        );
    }
}
""")

create(API + "/CustomerSubscriptionController.java", """package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.request.CreateCustomerSubscriptionRequest;
import com.sixpay.customer.management.api.request.SubscriptionReasonRequest;
import com.sixpay.customer.management.api.response.CustomerSubscriptionResponse;
import com.sixpay.customer.management.application.port.input.CustomerSubscriptionUseCase;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/subscriptions")
@Tag(
        name = "Customer Subscriptions",
        description = "Internal SIXPAY customer-partner subscription lifecycle"
)
@SecurityRequirement(name = "bearerAuth")
public class CustomerSubscriptionController {

    private final CustomerSubscriptionUseCase subscriptions;

    public CustomerSubscriptionController(
            CustomerSubscriptionUseCase subscriptions
    ) {
        this.subscriptions = subscriptions;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a pending customer subscription")
    public ResponseEntity<CustomerSubscriptionResponse> create(
            @Valid @RequestBody CreateCustomerSubscriptionRequest request
    ) {
        CustomerSubscriptionResponse response =
                CustomerSubscriptionResponse.from(
                        subscriptions.create(
                                new CustomerId(
                                        request.customerId()
                                ),
                                request.partnerId(),
                                new CustomerBankAccountId(
                                        request.bankAccountId()
                                ),
                                Instant.now()
                        )
                );

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{subscriptionId}/activation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or reactivate a subscription")
    public CustomerSubscriptionResponse activate(
            @PathVariable UUID subscriptionId
    ) {
        return CustomerSubscriptionResponse.from(
                subscriptions.activate(
                        new CustomerSubscriptionId(
                                subscriptionId
                        ),
                        Instant.now()
                )
        );
    }

    @PostMapping("/{subscriptionId}/suspension")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend an active subscription")
    public CustomerSubscriptionResponse suspend(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionReasonRequest request
    ) {
        return CustomerSubscriptionResponse.from(
                subscriptions.suspend(
                        new CustomerSubscriptionId(
                                subscriptionId
                        ),
                        request.reason(),
                        Instant.now()
                )
        );
    }

    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Close a subscription",
            description = "Logical close; history is retained"
    )
    public ResponseEntity<Void> close(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionReasonRequest request
    ) {
        subscriptions.close(
                new CustomerSubscriptionId(
                        subscriptionId
                ),
                request.reason(),
                Instant.now()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public CustomerSubscriptionResponse findById(
            @PathVariable UUID subscriptionId
    ) {
        return CustomerSubscriptionResponse.from(
                subscriptions.findById(
                        new CustomerSubscriptionId(
                                subscriptionId
                        )
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public List<CustomerSubscriptionResponse> findByCustomer(
            @RequestParam UUID customerId
    ) {
        return subscriptions.findByCustomerId(
                        new CustomerId(customerId)
                )
                .stream()
                .map(CustomerSubscriptionResponse::from)
                .toList();
    }
}
""")

create(BOOT + "/PartnerSubscriptionEligibilityAdapter.java", """package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.management.application.port.output.PartnerSubscriptionEligibilityPort;
import com.sixpay.partner.application.exception.PartnerNotFoundException;
import com.sixpay.partner.application.port.in.PartnerQueryUseCase;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public final class PartnerSubscriptionEligibilityAdapter
        implements PartnerSubscriptionEligibilityPort {

    private final PartnerQueryUseCase partnerQuery;

    public PartnerSubscriptionEligibilityAdapter(
            PartnerQueryUseCase partnerQuery
    ) {
        this.partnerQuery =
                Objects.requireNonNull(partnerQuery);
    }

    @Override
    public PartnerEligibility check(UUID partnerId) {
        try {
            var partner = partnerQuery.findById(
                    new PartnerId(partnerId)
            );

            return new PartnerEligibility(
                    true,
                    partner.status()
                            == PartnerStatus.ACTIVE
            );
        } catch (PartnerNotFoundException exception) {
            return new PartnerEligibility(
                    false,
                    false
            );
        }
    }
}
""")

create(TEST + "/CustomerSubscriptionTest.java", """package com.sixpay.customer.management.subscription;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerSubscriptionTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void followsActivationSuspensionReactivationAndClosureLifecycle() {
        CustomerSubscription subscription =
                CustomerSubscription.create(
                        new CustomerSubscriptionId(
                                UUID.randomUUID()
                        ),
                        new CustomerId(UUID.randomUUID()),
                        UUID.randomUUID(),
                        new CustomerBankAccountId(
                                UUID.randomUUID()
                        ),
                        NOW
                );

        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.PENDING_ACTIVATION
                );

        subscription.activate(NOW.plusSeconds(1));
        assertThat(subscription.acceptsPayments())
                .isTrue();

        subscription.suspend(
                "manual review",
                NOW.plusSeconds(2)
        );
        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.SUSPENDED
                );

        subscription.activate(NOW.plusSeconds(3));
        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.ACTIVE
                );

        subscription.close(
                "customer unsubscribed",
                NOW.plusSeconds(4)
        );
        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.CLOSED
                );

        assertThatThrownBy(() ->
                subscription.activate(
                        NOW.plusSeconds(5)
                )
        ).isInstanceOf(CustomerDomainException.class);
    }
}
""")

create(TEST + "/CustomerSubscriptionServiceTest.java", """package com.sixpay.customer.management.subscription;

import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.PartnerSubscriptionEligibilityPort;
import com.sixpay.customer.management.application.service.CustomerSubscriptionService;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.*;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CustomerSubscriptionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void createsPendingSubscriptionOnlyForActiveCustomerPartnerAndOwnedAccount() {
        CustomerRepository customers =
                mock(CustomerRepository.class);
        CustomerSubscriptionRepository subscriptions =
                mock(CustomerSubscriptionRepository.class);
        PartnerSubscriptionEligibilityPort partners =
                mock(PartnerSubscriptionEligibilityPort.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);

        Customer customer = customer();

        when(customers.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(partners.check(any()))
                .thenReturn(
                        new PartnerSubscriptionEligibilityPort
                                .PartnerEligibility(
                                        true,
                                        true
                                )
                );
        when(ids.nextId())
                .thenReturn(UUID.randomUUID());
        when(subscriptions.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        CustomerSubscriptionService service =
                new CustomerSubscriptionService(
                        customers,
                        subscriptions,
                        partners,
                        ids
                );

        CustomerSubscription subscription =
                service.create(
                        customer.id(),
                        UUID.randomUUID(),
                        customer.defaultBankAccount()
                                .orElseThrow()
                                .id(),
                        NOW
                );

        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.PENDING_ACTIVATION
                );

        verify(subscriptions).save(subscription);
    }

    @Test
    void refusesInactivePartner() {
        CustomerRepository customers =
                mock(CustomerRepository.class);
        CustomerSubscriptionRepository subscriptions =
                mock(CustomerSubscriptionRepository.class);
        PartnerSubscriptionEligibilityPort partners =
                mock(PartnerSubscriptionEligibilityPort.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);

        Customer customer = customer();

        when(customers.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(partners.check(any()))
                .thenReturn(
                        new PartnerSubscriptionEligibilityPort
                                .PartnerEligibility(
                                        true,
                                        false
                                )
                );

        CustomerSubscriptionService service =
                new CustomerSubscriptionService(
                        customers,
                        subscriptions,
                        partners,
                        ids
                );

        assertThatThrownBy(() ->
                service.create(
                        customer.id(),
                        UUID.randomUUID(),
                        customer.defaultBankAccount()
                                .orElseThrow()
                                .id(),
                        NOW
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "partner is not active"
                );

        verify(subscriptions, never()).save(any());
    }

    private static Customer customer() {
        CustomerId id =
                new CustomerId(UUID.randomUUID());

        return Customer.create(
                id,
                "SIXPAY_BANK",
                "BANK-CUSTOMER-001",
                "000123",
                "NIU-001",
                "Customer One",
                "customer@example.com",
                "+237600000001",
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                UUID.randomUUID()
                        ),
                        id,
                        "ACC-001",
                        "v1:" + "a".repeat(64),
                        "****0001",
                        "XAF",
                        "CURRENT",
                        NOW
                ),
                NOW
        );
    }
}
""")

print()
print("CM-5 subscription lifecycle created.")
print("Run:")
print("  ./mvnw -pl customer -am test")
print("  ./mvnw -pl bootstrap -am test")
print("  git diff --check")
print("  git status --short")
