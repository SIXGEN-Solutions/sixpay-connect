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
        raise SystemExit("Run inside the sixpay-connect Git repository.")
    repo = Path(r.stdout.strip()).resolve()
    if repo != ROOT.resolve():
        raise SystemExit(f"Run from repository root: {repo}")
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != BRANCH:
        raise SystemExit(f"Wrong branch: {branch!r}. Expected {BRANCH!r}.")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        current = p.read_text(encoding="utf-8")
        if current == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(
            f"[stop] {rel} already exists with different content; refusing overwrite"
        )
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

guard()

BASE = "backend/customer/src/main/java/com/sixpay/customer/management"
MODEL = BASE + "/domain/model"
REPO = BASE + "/domain/repository"
IN = BASE + "/application/port/input"
SVC = BASE + "/application/service"
PERSIST = BASE + "/infrastructure/persistence"
API = BASE + "/api"
REQ = API + "/request"
RESP = API + "/response"
MIG = "backend/customer/src/main/resources/db/migration"
TEST = "backend/customer/src/test/java/com/sixpay/customer/management/linking"

create(MODEL + "/ObservedCustomerLinkStatus.java", '''package com.sixpay.customer.management.domain.model;

public enum ObservedCustomerLinkStatus {
    LINKED,
    UNLINKED
}
''')

create(MODEL + "/ObservedCustomerLink.java", '''package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ObservedCustomerLink {

    private static final int MAX_ACTOR_LENGTH = 200;
    private static final int MAX_CORRELATION_LENGTH = 150;
    private static final int MAX_REASON_LENGTH = 500;

    private final UUID observedCustomerId;
    private CustomerId customerId;
    private ObservedCustomerLinkStatus status;

    private String linkedBy;
    private String linkCorrelationId;
    private String linkReason;
    private Instant linkedAt;

    private String unlinkedBy;
    private String unlinkCorrelationId;
    private String unlinkReason;
    private Instant unlinkedAt;

    private ObservedCustomerLink(
            UUID observedCustomerId,
            CustomerId customerId,
            ObservedCustomerLinkStatus status,
            String linkedBy,
            String linkCorrelationId,
            String linkReason,
            Instant linkedAt,
            String unlinkedBy,
            String unlinkCorrelationId,
            String unlinkReason,
            Instant unlinkedAt
    ) {
        this.observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
        this.customerId = Objects.requireNonNull(
                customerId,
                "customerId is required"
        );
        this.status = Objects.requireNonNull(
                status,
                "status is required"
        );
        this.linkedBy = requireText(
                linkedBy, "linkedBy", MAX_ACTOR_LENGTH
        );
        this.linkCorrelationId = requireText(
                linkCorrelationId,
                "linkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        this.linkReason = requireText(
                linkReason, "linkReason", MAX_REASON_LENGTH
        );
        this.linkedAt = Objects.requireNonNull(
                linkedAt,
                "linkedAt is required"
        );

        this.unlinkedBy = normalizeText(
                unlinkedBy, "unlinkedBy", MAX_ACTOR_LENGTH
        );
        this.unlinkCorrelationId = normalizeText(
                unlinkCorrelationId,
                "unlinkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        this.unlinkReason = normalizeText(
                unlinkReason,
                "unlinkReason",
                MAX_REASON_LENGTH
        );
        this.unlinkedAt = unlinkedAt;

        validateState();
    }

    public static ObservedCustomerLink create(
            UUID observedCustomerId,
            CustomerId customerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        return new ObservedCustomerLink(
                observedCustomerId,
                customerId,
                ObservedCustomerLinkStatus.LINKED,
                actorId,
                correlationId,
                reason,
                Objects.requireNonNull(now, "now is required"),
                null,
                null,
                null,
                null
        );
    }

    public static ObservedCustomerLink reconstitute(
            UUID observedCustomerId,
            CustomerId customerId,
            ObservedCustomerLinkStatus status,
            String linkedBy,
            String linkCorrelationId,
            String linkReason,
            Instant linkedAt,
            String unlinkedBy,
            String unlinkCorrelationId,
            String unlinkReason,
            Instant unlinkedAt
    ) {
        return new ObservedCustomerLink(
                observedCustomerId,
                customerId,
                status,
                linkedBy,
                linkCorrelationId,
                linkReason,
                linkedAt,
                unlinkedBy,
                unlinkCorrelationId,
                unlinkReason,
                unlinkedAt
        );
    }

    public void unlink(
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        if (status != ObservedCustomerLinkStatus.LINKED) {
            throw new CustomerDomainException(
                    "observed customer link is already unlinked"
            );
        }

        requireChronology(now);

        status = ObservedCustomerLinkStatus.UNLINKED;
        unlinkedBy = requireText(
                actorId, "unlinkedBy", MAX_ACTOR_LENGTH
        );
        unlinkCorrelationId = requireText(
                correlationId,
                "unlinkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        unlinkReason = requireText(
                reason, "unlinkReason", MAX_REASON_LENGTH
        );
        unlinkedAt = now;
    }

    public void relink(
            CustomerId targetCustomerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    ) {
        if (status != ObservedCustomerLinkStatus.UNLINKED) {
            throw new CustomerDomainException(
                    "observed customer is already linked"
            );
        }

        requireChronology(now);

        customerId = Objects.requireNonNull(
                targetCustomerId,
                "targetCustomerId is required"
        );
        status = ObservedCustomerLinkStatus.LINKED;
        linkedBy = requireText(
                actorId, "linkedBy", MAX_ACTOR_LENGTH
        );
        linkCorrelationId = requireText(
                correlationId,
                "linkCorrelationId",
                MAX_CORRELATION_LENGTH
        );
        linkReason = requireText(
                reason, "linkReason", MAX_REASON_LENGTH
        );
        linkedAt = now;

        unlinkedBy = null;
        unlinkCorrelationId = null;
        unlinkReason = null;
        unlinkedAt = null;
    }

    public boolean isLinked() {
        return status == ObservedCustomerLinkStatus.LINKED;
    }

    private void requireChronology(Instant now) {
        Objects.requireNonNull(now, "now is required");
        Instant latest = unlinkedAt == null ? linkedAt : unlinkedAt;

        if (now.isBefore(latest)) {
            throw new CustomerDomainException(
                    "link operation time must not precede previous link state"
            );
        }
    }

    private void validateState() {
        if (status == ObservedCustomerLinkStatus.LINKED) {
            if (unlinkedBy != null
                    || unlinkCorrelationId != null
                    || unlinkReason != null
                    || unlinkedAt != null) {
                throw new CustomerDomainException(
                        "LINKED correlation must not contain unlink metadata"
                );
            }
            return;
        }

        if (unlinkedBy == null
                || unlinkCorrelationId == null
                || unlinkReason == null
                || unlinkedAt == null) {
            throw new CustomerDomainException(
                    "UNLINKED correlation requires unlink metadata"
            );
        }

        if (unlinkedAt.isBefore(linkedAt)) {
            throw new CustomerDomainException(
                    "unlinkedAt must not precede linkedAt"
            );
        }
    }

    private static String requireText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new CustomerDomainException(
                    field + " is required"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new CustomerDomainException(
                    field + " must not exceed "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return requireText(value, field, maxLength);
    }

    public UUID observedCustomerId() {
        return observedCustomerId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public ObservedCustomerLinkStatus status() {
        return status;
    }

    public String linkedBy() {
        return linkedBy;
    }

    public String linkCorrelationId() {
        return linkCorrelationId;
    }

    public String linkReason() {
        return linkReason;
    }

    public Instant linkedAt() {
        return linkedAt;
    }

    public Optional<String> unlinkedBy() {
        return Optional.ofNullable(unlinkedBy);
    }

    public Optional<String> unlinkCorrelationId() {
        return Optional.ofNullable(unlinkCorrelationId);
    }

    public Optional<String> unlinkReason() {
        return Optional.ofNullable(unlinkReason);
    }

    public Optional<Instant> unlinkedAt() {
        return Optional.ofNullable(unlinkedAt);
    }
}
''')

create(REPO + "/ObservedCustomerLinkRepository.java", '''package com.sixpay.customer.management.domain.repository;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservedCustomerLinkRepository {

    ObservedCustomerLink save(ObservedCustomerLink link);

    Optional<ObservedCustomerLink> findByObservedCustomerId(
            UUID observedCustomerId
    );

    List<ObservedCustomerLink> findLinkedByCustomerId(
            CustomerId customerId
    );
}
''')

create(IN + "/ObservedCustomerLinkUseCase.java", '''package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservedCustomerLinkUseCase {

    ObservedCustomerLink link(
            UUID observedCustomerId,
            CustomerId customerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    );

    ObservedCustomerLink unlink(
            UUID observedCustomerId,
            String actorId,
            String correlationId,
            String reason,
            Instant now
    );

    Optional<ObservedCustomerLink> findLinked(
            UUID observedCustomerId
    );

    List<ObservedCustomerLink> findByCustomerId(
            CustomerId customerId
    );
}
''')

create(SVC + "/ObservedCustomerLinkService.java", '''package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.ObservedCustomerLinkUseCase;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.ObservedCustomerLinkRepository;
import com.sixpay.customer.observation.application.port.input.query.GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.query.GetObservedCustomerQuery;
import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public final class ObservedCustomerLinkService
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
''')

create(PERSIST + "/ObservedCustomerLinkJpaEntity.java", '''package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
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
@Table(name = "customer_observed_master_link")
public class ObservedCustomerLinkJpaEntity {

    @Id
    @Column(
            name = "observed_customer_id",
            nullable = false,
            updatable = false
    )
    private UUID observedCustomerId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_status", nullable = false, length = 16)
    private ObservedCustomerLinkStatus status;

    @Column(name = "linked_by", nullable = false, length = 200)
    private String linkedBy;

    @Column(
            name = "link_correlation_id",
            nullable = false,
            length = 150
    )
    private String linkCorrelationId;

    @Column(name = "link_reason", nullable = false, length = 500)
    private String linkReason;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "unlinked_by", length = 200)
    private String unlinkedBy;

    @Column(name = "unlink_correlation_id", length = 150)
    private String unlinkCorrelationId;

    @Column(name = "unlink_reason", length = 500)
    private String unlinkReason;

    @Column(name = "unlinked_at")
    private Instant unlinkedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected ObservedCustomerLinkJpaEntity() {
    }

    static ObservedCustomerLinkJpaEntity create(
            ObservedCustomerLink link
    ) {
        ObservedCustomerLinkJpaEntity entity =
                new ObservedCustomerLinkJpaEntity();
        entity.observedCustomerId =
                link.observedCustomerId();
        entity.synchronize(link);
        return entity;
    }

    void synchronize(ObservedCustomerLink link) {
        customerId = link.customerId().value();
        status = link.status();
        linkedBy = link.linkedBy();
        linkCorrelationId = link.linkCorrelationId();
        linkReason = link.linkReason();
        linkedAt = link.linkedAt();
        unlinkedBy = link.unlinkedBy().orElse(null);
        unlinkCorrelationId =
                link.unlinkCorrelationId().orElse(null);
        unlinkReason = link.unlinkReason().orElse(null);
        unlinkedAt = link.unlinkedAt().orElse(null);
    }

    UUID observedCustomerId() { return observedCustomerId; }
    UUID customerId() { return customerId; }
    ObservedCustomerLinkStatus status() { return status; }
    String linkedBy() { return linkedBy; }
    String linkCorrelationId() { return linkCorrelationId; }
    String linkReason() { return linkReason; }
    Instant linkedAt() { return linkedAt; }
    String unlinkedBy() { return unlinkedBy; }
    String unlinkCorrelationId() { return unlinkCorrelationId; }
    String unlinkReason() { return unlinkReason; }
    Instant unlinkedAt() { return unlinkedAt; }
}
''')

create(PERSIST + "/ObservedCustomerLinkSpringDataRepository.java", '''package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservedCustomerLinkSpringDataRepository
        extends JpaRepository<ObservedCustomerLinkJpaEntity, UUID> {

    List<ObservedCustomerLinkJpaEntity>
            findByCustomerIdAndStatusOrderByLinkedAtDesc(
                    UUID customerId,
                    ObservedCustomerLinkStatus status
            );
}
''')

create(PERSIST + "/ObservedCustomerLinkRepositoryAdapter.java", '''package com.sixpay.customer.management.infrastructure.persistence;

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
''')

create(MIG + "/V20260822.03__link_observed_customer_to_customer.sql", '''CREATE TABLE customer_observed_master_link (
    observed_customer_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    link_status VARCHAR(16) NOT NULL,

    linked_by VARCHAR(200) NOT NULL,
    link_correlation_id VARCHAR(150) NOT NULL,
    link_reason VARCHAR(500) NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL,

    unlinked_by VARCHAR(200),
    unlink_correlation_id VARCHAR(150),
    unlink_reason VARCHAR(500),
    unlinked_at TIMESTAMPTZ,

    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_observed_master_link_observed
        FOREIGN KEY (observed_customer_id)
        REFERENCES customer_observed_customer (observed_customer_id),

    CONSTRAINT fk_observed_master_link_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer_management_customer (customer_id),

    CONSTRAINT ck_observed_master_link_status
        CHECK (link_status IN ('LINKED', 'UNLINKED')),

    CONSTRAINT ck_observed_master_link_state
        CHECK (
            (
                link_status = 'LINKED'
                AND unlinked_by IS NULL
                AND unlink_correlation_id IS NULL
                AND unlink_reason IS NULL
                AND unlinked_at IS NULL
            )
            OR
            (
                link_status = 'UNLINKED'
                AND unlinked_by IS NOT NULL
                AND unlink_correlation_id IS NOT NULL
                AND unlink_reason IS NOT NULL
                AND unlinked_at IS NOT NULL
                AND unlinked_at >= linked_at
            )
        ),

    CONSTRAINT ck_observed_master_link_row_version
        CHECK (row_version >= 0)
);

CREATE INDEX ix_observed_master_link_customer
    ON customer_observed_master_link (
        customer_id,
        link_status,
        linked_at DESC
    );
''')

create(REQ + "/LinkObservedCustomerRequest.java", '''package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LinkObservedCustomerRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 500) String reason
) {
}
''')

create(REQ + "/UnlinkObservedCustomerRequest.java", '''package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnlinkObservedCustomerRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
''')

create(RESP + "/ObservedCustomerLinkResponse.java", '''package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.ObservedCustomerLink;

import java.time.Instant;
import java.util.UUID;

public record ObservedCustomerLinkResponse(
        UUID observedCustomerId,
        UUID customerId,
        String status,
        String linkedBy,
        String correlationId,
        String reason,
        Instant linkedAt
) {
    public static ObservedCustomerLinkResponse from(
            ObservedCustomerLink link
    ) {
        return new ObservedCustomerLinkResponse(
                link.observedCustomerId(),
                link.customerId().value(),
                link.status().name(),
                link.linkedBy(),
                link.linkCorrelationId(),
                link.linkReason(),
                link.linkedAt()
        );
    }
}
''')

create(API + "/ObservedCustomerLinkController.java", '''package com.sixpay.customer.management.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.management.api.request.LinkObservedCustomerRequest;
import com.sixpay.customer.management.api.request.UnlinkObservedCustomerRequest;
import com.sixpay.customer.management.api.response.ObservedCustomerLinkResponse;
import com.sixpay.customer.management.application.port.input.ObservedCustomerLinkUseCase;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.security.authentication.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/observed-customers")
@Tag(
        name = "Observed Customer Linking",
        description = "Explicit optional correlation between observed projections and Customer master data"
)
@SecurityRequirement(name = "bearerAuth")
public class ObservedCustomerLinkController {

    private static final String CORRELATION_HEADER =
            "X-Correlation-ID";
    private static final int HEADER_MAX_LENGTH = 150;

    private final ObservedCustomerLinkUseCase links;
    private final CurrentUserProvider currentUserProvider;

    public ObservedCustomerLinkController(
            ObservedCustomerLinkUseCase links,
            CurrentUserProvider currentUserProvider
    ) {
        this.links = links;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/{observedCustomerId}/customer-link")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Explicitly link an ObservedCustomer to an enrolled Customer",
            description = "Does not create or update Customer master data"
    )
    public ObservedCustomerLinkResponse link(
            @PathVariable UUID observedCustomerId,
            @Valid @RequestBody LinkObservedCustomerRequest request,
            @RequestHeader(
                    name = CORRELATION_HEADER,
                    required = false
            )
            @Size(max = HEADER_MAX_LENGTH)
            String correlationId
    ) {
        return ObservedCustomerLinkResponse.from(
                links.link(
                        observedCustomerId,
                        new CustomerId(request.customerId()),
                        actor(),
                        correlation(correlationId),
                        request.reason(),
                        Instant.now()
                )
        );
    }

    @DeleteMapping("/{observedCustomerId}/customer-link")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove the active Customer correlation",
            description = "The ObservedCustomer projection remains unchanged"
    )
    public ResponseEntity<Void> unlink(
            @PathVariable UUID observedCustomerId,
            @Valid @RequestBody UnlinkObservedCustomerRequest request,
            @RequestHeader(
                    name = CORRELATION_HEADER,
                    required = false
            )
            @Size(max = HEADER_MAX_LENGTH)
            String correlationId
    ) {
        links.unlink(
                observedCustomerId,
                actor(),
                correlation(correlationId),
                request.reason(),
                Instant.now()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{observedCustomerId}/customer-link")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
    )
    @Operation(summary = "Get the active Customer correlation")
    public ResponseEntity<ObservedCustomerLinkResponse> find(
            @PathVariable UUID observedCustomerId
    ) {
        return links.findLinked(observedCustomerId)
                .map(ObservedCustomerLinkResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/customer-links")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
    )
    @Operation(
            summary = "List observed projections explicitly linked to one Customer"
    )
    public List<ObservedCustomerLinkResponse> findByCustomer(
            @RequestParam UUID customerId
    ) {
        return links.findByCustomerId(
                        new CustomerId(customerId)
                )
                .stream()
                .map(ObservedCustomerLinkResponse::from)
                .toList();
    }

    private String actor() {
        return currentUserProvider
                .requireCurrentUser()
                .subject();
    }

    private static String correlation(
            String correlationId
    ) {
        return (
                correlationId == null
                        || correlationId.isBlank()
        )
                ? CorrelationId.generate().value()
                : CorrelationId.of(
                        correlationId.strip()
                ).value();
    }
}
''')

create(API + "/ObservedCustomerLinkApiExceptionHandler.java", '''package com.sixpay.customer.management.api;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = ObservedCustomerLinkController.class
)
public class ObservedCustomerLinkApiExceptionHandler {

    @ExceptionHandler(CustomerDomainException.class)
    ProblemDetail domain(
            CustomerDomainException exception
    ) {
        String message = exception.getMessage();

        HttpStatus status =
                message != null
                        && (
                        message.startsWith("customer not found")
                                || message.startsWith(
                                "observed customer link not found"
                        )
                )
                        ? HttpStatus.NOT_FOUND
                        : HttpStatus.CONFLICT;

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        status,
                        message == null
                                ? "Observed Customer link operation failed"
                                : message
                );

        detail.setTitle(
                "Observed Customer linking error"
        );

        return detail;
    }
}
''')

create(TEST + "/ObservedCustomerLinkTest.java", '''package com.sixpay.customer.management.linking;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.model.ObservedCustomerLinkStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservedCustomerLinkTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void supportsExplicitLinkUnlinkAndRelink() {
        UUID observedCustomerId = UUID.randomUUID();
        CustomerId firstCustomer =
                new CustomerId(UUID.randomUUID());
        CustomerId secondCustomer =
                new CustomerId(UUID.randomUUID());

        ObservedCustomerLink link =
                ObservedCustomerLink.create(
                        observedCustomerId,
                        firstCustomer,
                        "admin-user",
                        "corr-1",
                        "manual correlation confirmed",
                        NOW
                );

        assertThat(link.status())
                .isEqualTo(
                        ObservedCustomerLinkStatus.LINKED
                );

        link.unlink(
                "admin-user",
                "corr-2",
                "correlation invalidated",
                NOW.plusSeconds(1)
        );

        assertThat(link.status())
                .isEqualTo(
                        ObservedCustomerLinkStatus.UNLINKED
                );

        link.relink(
                secondCustomer,
                "admin-user",
                "corr-3",
                "corrected correlation",
                NOW.plusSeconds(2)
        );

        assertThat(link.status())
                .isEqualTo(
                        ObservedCustomerLinkStatus.LINKED
                );
        assertThat(link.customerId())
                .isEqualTo(secondCustomer);
        assertThat(link.unlinkedAt())
                .isEmpty();
    }

    @Test
    void refusesUnlinkTwice() {
        ObservedCustomerLink link =
                ObservedCustomerLink.create(
                        UUID.randomUUID(),
                        new CustomerId(UUID.randomUUID()),
                        "admin-user",
                        "corr-1",
                        "manual link",
                        NOW
                );

        link.unlink(
                "admin-user",
                "corr-2",
                "manual unlink",
                NOW.plusSeconds(1)
        );

        assertThatThrownBy(() ->
                link.unlink(
                        "admin-user",
                        "corr-3",
                        "duplicate unlink",
                        NOW.plusSeconds(2)
                )
        ).isInstanceOf(CustomerDomainException.class);
    }
}
''')

create(TEST + "/ObservedCustomerLinkServiceTest.java", '''package com.sixpay.customer.management.linking;

import com.sixpay.customer.management.application.service.ObservedCustomerLinkService;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.ObservedCustomerLink;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.ObservedCustomerLinkRepository;
import com.sixpay.customer.observation.application.port.input.query.GetObservedCustomerUseCase;
import com.sixpay.customer.observation.application.query.ObservedCustomerDetailView;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ObservedCustomerLinkServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void explicitlyLinksExistingObservationToExistingMasterCustomer() {
        GetObservedCustomerUseCase observed =
                mock(GetObservedCustomerUseCase.class);
        CustomerRepository customers =
                mock(CustomerRepository.class);
        ObservedCustomerLinkRepository links =
                mock(ObservedCustomerLinkRepository.class);

        UUID observedCustomerId = UUID.randomUUID();
        CustomerId customerId =
                new CustomerId(UUID.randomUUID());

        when(observed.get(any()))
                .thenReturn(mock(ObservedCustomerDetailView.class));
        when(customers.existsById(customerId))
                .thenReturn(true);
        when(links.findByObservedCustomerId(
                observedCustomerId
        )).thenReturn(Optional.empty());
        when(links.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        ObservedCustomerLinkService service =
                new ObservedCustomerLinkService(
                        observed,
                        customers,
                        links
                );

        ObservedCustomerLink link =
                service.link(
                        observedCustomerId,
                        customerId,
                        "admin-user",
                        "corr-1",
                        "manual correlation confirmed",
                        NOW
                );

        assertThat(link.customerId())
                .isEqualTo(customerId);

        verify(observed).get(any());
        verify(customers).existsById(customerId);
        verify(links).save(link);
        verify(customers, never()).save(any());
    }

    @Test
    void neverCreatesMasterDataWhenTargetCustomerDoesNotExist() {
        GetObservedCustomerUseCase observed =
                mock(GetObservedCustomerUseCase.class);
        CustomerRepository customers =
                mock(CustomerRepository.class);
        ObservedCustomerLinkRepository links =
                mock(ObservedCustomerLinkRepository.class);

        CustomerId missingCustomer =
                new CustomerId(UUID.randomUUID());

        when(observed.get(any()))
                .thenReturn(mock(ObservedCustomerDetailView.class));
        when(customers.existsById(missingCustomer))
                .thenReturn(false);

        ObservedCustomerLinkService service =
                new ObservedCustomerLinkService(
                        observed,
                        customers,
                        links
                );

        assertThatThrownBy(() ->
                service.link(
                        UUID.randomUUID(),
                        missingCustomer,
                        "admin-user",
                        "corr-1",
                        "manual correlation",
                        NOW
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "customer not found"
                );

        verify(customers, never()).save(any());
        verifyNoInteractions(links);
    }

    @Test
    void refusesSilentRelinkToDifferentCustomer() {
        GetObservedCustomerUseCase observed =
                mock(GetObservedCustomerUseCase.class);
        CustomerRepository customers =
                mock(CustomerRepository.class);
        ObservedCustomerLinkRepository links =
                mock(ObservedCustomerLinkRepository.class);

        UUID observedCustomerId = UUID.randomUUID();
        CustomerId firstCustomer =
                new CustomerId(UUID.randomUUID());
        CustomerId secondCustomer =
                new CustomerId(UUID.randomUUID());

        ObservedCustomerLink current =
                ObservedCustomerLink.create(
                        observedCustomerId,
                        firstCustomer,
                        "admin-user",
                        "corr-1",
                        "first correlation",
                        NOW
                );

        when(observed.get(any()))
                .thenReturn(mock(ObservedCustomerDetailView.class));
        when(customers.existsById(secondCustomer))
                .thenReturn(true);
        when(links.findByObservedCustomerId(
                observedCustomerId
        )).thenReturn(Optional.of(current));

        ObservedCustomerLinkService service =
                new ObservedCustomerLinkService(
                        observed,
                        customers,
                        links
                );

        assertThatThrownBy(() ->
                service.link(
                        observedCustomerId,
                        secondCustomer,
                        "admin-user",
                        "corr-2",
                        "attempted replacement",
                        NOW.plusSeconds(1)
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "unlink first"
                );

        verify(links, never()).save(any());
    }
}
''')

print()
print("CM-6 Observed Customer linking implementation created.")
print("Run:")
print("  ./mvnw -pl customer -am test")
print("  ./mvnw -pl customer -am verify -Pfull-tests")
print("  git diff --check")
print("  git status --short")
