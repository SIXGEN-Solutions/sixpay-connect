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
    repo = Path(r.stdout.strip()).resolve()
    if repo != ROOT.resolve():
        raise SystemExit(f"Run from repository root: {repo}")
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != BRANCH:
        raise SystemExit(f"Expected {BRANCH}, got {branch}")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        if p.read_text(encoding="utf-8") == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(f"[stop] {rel} exists with different content")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

def replace_all(rel, replacements):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"[stop] missing {rel}")
    text = p.read_text(encoding="utf-8")
    changed = False
    for old, new, label in replacements:
        if old in text:
            text = text.replace(old, new)
            changed = True
            print(f"[update] {rel}: {label}")
        elif new in text:
            print(f"[skip] {rel}: {label}")
        else:
            print(f"[warn] {rel}: pattern not found for {label}")
    if changed:
        p.write_text(text, encoding="utf-8", newline="\n")

guard()

SEC = "backend/security/src/main/java/com/sixpay/security/authorization/SixpayPermission.java"
CUSTOMER = "backend/customer/src/main/java/com/sixpay/customer/management"
AUDIT = CUSTOMER + "/application/audit"
OUT = CUSTOMER + "/application/port/output"
INFRA = CUSTOMER + "/infrastructure/audit"
API = CUSTOMER + "/api"
RESP = API + "/response"
MIG = "backend/customer/src/main/resources/db/migration"

# ------------------------------------------------------------------
# 1. Canonical business permissions
# ------------------------------------------------------------------
replace_all(SEC, [(
'''    OBSERVED_CUSTOMER_READ("observed-customer.read"),

    PAYMENT_READ("payment.read"),''',
'''    OBSERVED_CUSTOMER_READ("observed-customer.read"),

    CUSTOMER_READ("customer.read"),
    CUSTOMER_CREATE("customer.create"),
    CUSTOMER_UPDATE("customer.update"),
    CUSTOMER_SUSPEND("customer.suspend"),
    CUSTOMER_AUDIT_READ("customer.audit.read"),

    SUBSCRIPTION_READ("subscription.read"),
    SUBSCRIPTION_CREATE("subscription.create"),
    SUBSCRIPTION_UPDATE("subscription.update"),
    SUBSCRIPTION_SUSPEND("subscription.suspend"),
    SUBSCRIPTION_CLOSE("subscription.close"),

    PAYMENT_READ("payment.read"),''',
"add customer/subscription permissions"
)])

# ------------------------------------------------------------------
# 2. Replace role-based API guards with permission authorities
# ------------------------------------------------------------------
replace_all(API + "/CustomerController.java", [
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Enroll a verified banking customer into SIXPAY")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.create\')")\n    @Operation(summary = "Enroll a verified banking customer into SIXPAY")',
     "customer create permission"),
    ('@PreAuthorize("hasAnyRole(\'ADMIN\', \'MANAGER\', \'AUDITOR\')")\n    @Operation(summary = "Get a SIXPAY customer")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.read\')")\n    @Operation(summary = "Get a SIXPAY customer")',
     "customer read permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Update editable SIXPAY customer profile fields")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")\n    @Operation(summary = "Update editable SIXPAY customer profile fields")',
     "customer update permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Suspend a SIXPAY customer")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.suspend\')")\n    @Operation(summary = "Suspend a SIXPAY customer")',
     "customer suspend permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Reactivate a suspended SIXPAY customer")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")\n    @Operation(summary = "Reactivate a suspended SIXPAY customer")',
     "customer reactivate permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(\n            summary = "Close a SIXPAY customer"',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")\n    @Operation(\n            summary = "Close a SIXPAY customer"',
     "customer close permission"),
    ('@PreAuthorize("hasAnyRole(\'ADMIN\', \'MANAGER\', \'AUDITOR\')")\n    @Operation(summary = "List customer bank accounts")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.read\')")\n    @Operation(summary = "List customer bank accounts")',
     "account read permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(\n            summary = "Lookup, freshly verify and link a bank account to a customer"',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")\n    @Operation(\n            summary = "Lookup, freshly verify and link a bank account to a customer"',
     "account add permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Set the default customer bank account")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")\n    @Operation(summary = "Set the default customer bank account")',
     "default account permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Unlink a bank account from a customer")',
     '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")\n    @Operation(summary = "Unlink a bank account from a customer")',
     "account remove permission"),
])

replace_all(API + "/CustomerSubscriptionController.java", [
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Create a pending customer subscription")',
     '@PreAuthorize("hasAuthority(\'SCOPE_subscription.create\')")\n    @Operation(summary = "Create a pending customer subscription")',
     "subscription create permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Activate or reactivate a subscription")',
     '@PreAuthorize("hasAuthority(\'SCOPE_subscription.update\')")\n    @Operation(summary = "Activate or reactivate a subscription")',
     "subscription activate permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(summary = "Suspend an active subscription")',
     '@PreAuthorize("hasAuthority(\'SCOPE_subscription.suspend\')")\n    @Operation(summary = "Suspend an active subscription")',
     "subscription suspend permission"),
    ('@PreAuthorize("hasRole(\'ADMIN\')")\n    @Operation(\n            summary = "Close a subscription"',
     '@PreAuthorize("hasAuthority(\'SCOPE_subscription.close\')")\n    @Operation(\n            summary = "Close a subscription"',
     "subscription close permission"),
    ('@PreAuthorize("hasAnyRole(\'ADMIN\', \'MANAGER\', \'AUDITOR\')")',
     '@PreAuthorize("hasAuthority(\'SCOPE_subscription.read\')")',
     "subscription read permissions"),
])

link_controller = ROOT / (API + "/ObservedCustomerLinkController.java")
if link_controller.exists():
    replace_all(API + "/ObservedCustomerLinkController.java", [
        ('@PreAuthorize("hasRole(\'ADMIN\')")',
         '@PreAuthorize("hasAuthority(\'SCOPE_customer.update\')")',
         "observed link mutation permission"),
        ('@PreAuthorize(\n            "hasAnyRole(\'ADMIN\', \'MANAGER\', \'AUDITOR\')"\n    )',
         '@PreAuthorize("hasAuthority(\'SCOPE_customer.read\')")',
         "observed link read permission"),
    ])

# ------------------------------------------------------------------
# 3. Generic immutable audit contract, golden-module style
# ------------------------------------------------------------------
create(AUDIT + "/CustomerAuditRecord.java", '''package com.sixpay.customer.management.application.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerAuditRecord(
        UUID auditId,
        String aggregateType,
        UUID aggregateId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {
    public CustomerAuditRecord {
        Objects.requireNonNull(auditId, "auditId is required");
        aggregateType = requireText(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        action = requireText(action, "action");
        result = requireText(result, "result");
        actorId = requireText(actorId, "actorId");
        correlationId = requireText(correlationId, "correlationId");
        details = requireText(details, "details");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
''')

create(OUT + "/CustomerAuditTrail.java", '''package com.sixpay.customer.management.application.port.output;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerAuditTrail {

    void append(CustomerAuditRecord record);

    List<CustomerAuditRecord> find(
            String aggregateType,
            UUID aggregateId,
            Instant from,
            Instant to
    );
}
''')

create(INFRA + "/CustomerAuditJpaEntity.java", '''package com.sixpay.customer.management.infrastructure.audit;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_management_audit")
public class CustomerAuditJpaEntity {

    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "result", nullable = false, length = 32)
    private String result;

    @Column(name = "actor_id", nullable = false, length = 200)
    private String actorId;

    @Column(name = "correlation_id", nullable = false, length = 150)
    private String correlationId;

    @Column(name = "details", nullable = false, length = 2000)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected CustomerAuditJpaEntity() {
    }

    CustomerAuditJpaEntity(CustomerAuditRecord record) {
        auditId = record.auditId();
        aggregateType = record.aggregateType();
        aggregateId = record.aggregateId();
        action = record.action();
        result = record.result();
        actorId = record.actorId();
        correlationId = record.correlationId();
        details = record.details();
        occurredAt = record.occurredAt();
    }

    CustomerAuditRecord toRecord() {
        return new CustomerAuditRecord(
                auditId,
                aggregateType,
                aggregateId,
                action,
                result,
                actorId,
                correlationId,
                details,
                occurredAt
        );
    }
}
''')

create(INFRA + "/CustomerAuditSpringDataRepository.java", '''package com.sixpay.customer.management.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerAuditSpringDataRepository
        extends JpaRepository<CustomerAuditJpaEntity, UUID> {

    List<CustomerAuditJpaEntity>
            findByAggregateTypeAndAggregateIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                    String aggregateType,
                    UUID aggregateId,
                    Instant from,
                    Instant to
            );
}
''')

create(INFRA + "/CustomerAuditTrailAdapter.java", '''package com.sixpay.customer.management.infrastructure.audit;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;
import com.sixpay.customer.management.application.port.output.CustomerAuditTrail;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class CustomerAuditTrailAdapter
        implements CustomerAuditTrail {

    private final CustomerAuditSpringDataRepository repository;

    public CustomerAuditTrailAdapter(
            CustomerAuditSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void append(CustomerAuditRecord record) {
        repository.save(
                new CustomerAuditJpaEntity(record)
        );
    }

    @Override
    public List<CustomerAuditRecord> find(
            String aggregateType,
            UUID aggregateId,
            Instant from,
            Instant to
    ) {
        return repository
                .findByAggregateTypeAndAggregateIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        aggregateType,
                        aggregateId,
                        from,
                        to
                )
                .stream()
                .map(CustomerAuditJpaEntity::toRecord)
                .toList();
    }
}
''')

# ------------------------------------------------------------------
# 4. Audit recorder. Controllers call it only after successful mutation.
# This preserves existing use-case signatures and avoids destabilizing CM1-CM6.
# ------------------------------------------------------------------
create(AUDIT + "/CustomerAuditRecorder.java", '''package com.sixpay.customer.management.application.audit;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.management.application.port.output.CustomerAuditTrail;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CustomerAuditRecorder {

    private final CustomerAuditTrail auditTrail;
    private final CurrentUserProvider currentUserProvider;

    public CustomerAuditRecorder(
            CustomerAuditTrail auditTrail,
            CurrentUserProvider currentUserProvider
    ) {
        this.auditTrail = auditTrail;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void success(
            String aggregateType,
            UUID aggregateId,
            String action,
            String correlationId,
            String details
    ) {
        String actor = currentUserProvider
                .requireCurrentUser()
                .subject();

        String effectiveCorrelationId =
                correlationId == null || correlationId.isBlank()
                        ? CorrelationId.generate().value()
                        : CorrelationId.of(
                                correlationId.strip()
                        ).value();

        auditTrail.append(
                new CustomerAuditRecord(
                        UUID.randomUUID(),
                        aggregateType,
                        aggregateId,
                        action,
                        "SUCCESS",
                        actor,
                        effectiveCorrelationId,
                        details,
                        Instant.now()
                )
        );
    }
}
''')

create(RESP + "/CustomerAuditRecordResponse.java", '''package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;

import java.time.Instant;
import java.util.UUID;

public record CustomerAuditRecordResponse(
        UUID auditId,
        String aggregateType,
        UUID aggregateId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {
    public static CustomerAuditRecordResponse from(
            CustomerAuditRecord record
    ) {
        return new CustomerAuditRecordResponse(
                record.auditId(),
                record.aggregateType(),
                record.aggregateId(),
                record.action(),
                record.result(),
                record.actorId(),
                record.correlationId(),
                record.details(),
                record.occurredAt()
        );
    }
}
''')

create(API + "/CustomerAuditController.java", '''package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.response.CustomerAuditRecordResponse;
import com.sixpay.customer.management.application.port.output.CustomerAuditTrail;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/customer-audit-records")
public class CustomerAuditController {

    private final CustomerAuditTrail auditTrail;

    public CustomerAuditController(
            CustomerAuditTrail auditTrail
    ) {
        this.auditTrail = auditTrail;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_customer.audit.read')")
    public List<CustomerAuditRecordResponse> find(
            @RequestParam String aggregateType,
            @RequestParam UUID aggregateId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "from must be before or equal to to"
            );
        }

        return auditTrail.find(
                        aggregateType,
                        aggregateId,
                        from,
                        to
                )
                .stream()
                .map(CustomerAuditRecordResponse::from)
                .toList();
    }
}
''')

create(MIG + "/V20260822.04__create_customer_management_audit.sql", '''CREATE TABLE customer_management_audit (
    audit_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    action VARCHAR(100) NOT NULL,
    result VARCHAR(32) NOT NULL,
    actor_id VARCHAR(200) NOT NULL,
    correlation_id VARCHAR(150) NOT NULL,
    details VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_customer_management_audit_result
        CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX ix_customer_management_audit_aggregate
    ON customer_management_audit (
        aggregate_type,
        aggregate_id,
        occurred_at
    );

CREATE INDEX ix_customer_management_audit_actor
    ON customer_management_audit (
        actor_id,
        occurred_at
    );

CREATE INDEX ix_customer_management_audit_correlation
    ON customer_management_audit (
        correlation_id
    );
''')

# ------------------------------------------------------------------
# 5. Add audit calls to CustomerController with minimal signature churn.
# ------------------------------------------------------------------
customer_controller = ROOT / (API + "/CustomerController.java")
if customer_controller.exists():
    text = customer_controller.read_text(encoding="utf-8")
    if "CustomerAuditRecorder" not in text:
        text = text.replace(
            "import com.sixpay.customer.management.api.response.CustomerBankAccountResponse;\n",
            "import com.sixpay.customer.management.api.response.CustomerBankAccountResponse;\n"
            "import com.sixpay.customer.management.application.audit.CustomerAuditRecorder;\n"
        )
        text = text.replace(
            "    private final CustomerQueryUseCase query;\n",
            "    private final CustomerQueryUseCase query;\n"
            "    private final CustomerAuditRecorder audit;\n"
        )
        text = text.replace(
            "            CustomerManagementUseCase management,\n"
            "            CustomerQueryUseCase query\n"
            "    ) {\n"
            "        this.enrollment = enrollment;\n"
            "        this.management = management;\n"
            "        this.query = query;\n",
            "            CustomerManagementUseCase management,\n"
            "            CustomerQueryUseCase query,\n"
            "            CustomerAuditRecorder audit\n"
            "    ) {\n"
            "        this.enrollment = enrollment;\n"
            "        this.management = management;\n"
            "        this.query = query;\n"
            "        this.audit = audit;\n"
        )

        # create
        old = "        return ResponseEntity.created(location).body(response);\n"
        new = '''        audit.success(
                "CUSTOMER",
                response.id(),
                "CUSTOMER_CREATED",
                effectiveCorrelationId,
                "Customer enrolled after fresh banking verification"
        );

        return ResponseEntity.created(location).body(response);
'''
        text = text.replace(old, new, 1)

        # add account has correlation available
        old = '''        return CustomerResponse.from(
                management.addBankAccount(
                        new CustomerId(customerId),
                        new AddBankAccountCommand(
                                request.accountReference(),
                                effectiveCorrelationId
                        ),
                        Instant.now()
                )
        );
'''
        new = '''        CustomerResponse response = CustomerResponse.from(
                management.addBankAccount(
                        new CustomerId(customerId),
                        new AddBankAccountCommand(
                                request.accountReference(),
                                effectiveCorrelationId
                        ),
                        Instant.now()
                )
        );

        audit.success(
                "CUSTOMER",
                customerId,
                "CUSTOMER_BANK_ACCOUNT_LINKED",
                effectiveCorrelationId,
                "Verified bank account linked to Customer"
        );

        return response;
'''
        text = text.replace(old, new, 1)

        customer_controller.write_text(text, encoding="utf-8", newline="\n")
        print("[update] CustomerController audit wiring")
    else:
        print("[skip] CustomerController audit wiring")

# Subscription auditing: add CurrentUser-neutral recorder and generated correlation.
subscription_controller = ROOT / (API + "/CustomerSubscriptionController.java")
if subscription_controller.exists():
    text = subscription_controller.read_text(encoding="utf-8")
    if "CustomerAuditRecorder" not in text:
        text = text.replace(
            "import com.sixpay.customer.management.api.response.CustomerSubscriptionResponse;\n",
            "import com.sixpay.customer.management.api.response.CustomerSubscriptionResponse;\n"
            "import com.sixpay.customer.management.application.audit.CustomerAuditRecorder;\n"
        )
        text = text.replace(
            "    private final CustomerSubscriptionUseCase subscriptions;\n",
            "    private final CustomerSubscriptionUseCase subscriptions;\n"
            "    private final CustomerAuditRecorder audit;\n"
        )
        text = text.replace(
            "    public CustomerSubscriptionController(\n"
            "            CustomerSubscriptionUseCase subscriptions\n"
            "    ) {\n"
            "        this.subscriptions = subscriptions;\n"
            "    }\n",
            "    public CustomerSubscriptionController(\n"
            "            CustomerSubscriptionUseCase subscriptions,\n"
            "            CustomerAuditRecorder audit\n"
            "    ) {\n"
            "        this.subscriptions = subscriptions;\n"
            "        this.audit = audit;\n"
            "    }\n"
        )
        text = text.replace(
            "        return ResponseEntity.created(location).body(response);\n",
            '''        audit.success(
                "SUBSCRIPTION",
                response.id(),
                "SUBSCRIPTION_CREATED",
                null,
                "Customer subscription created in pending activation status"
        );

        return ResponseEntity.created(location).body(response);
''',
            1
        )
        subscription_controller.write_text(text, encoding="utf-8", newline="\n")
        print("[update] CustomerSubscriptionController audit wiring")
    else:
        print("[skip] CustomerSubscriptionController audit wiring")

print()
print("CM-7 security and audit baseline applied.")
print("Validate with:")
print("  ./mvnw -pl security -am test")
print("  ./mvnw -pl customer -am test")
print("  ./mvnw -pl customer -am verify -Pfull-tests")
print("  git diff --check")
print("  git status --short")
