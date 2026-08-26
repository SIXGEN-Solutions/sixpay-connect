#!/usr/bin/env python3
"""Normalize module README files to concise current-state documentation.

The script rewrites only README files, keeps implementation and historical
documents untouched, and never commits or pushes changes.
"""

from __future__ import annotations

import argparse
import shutil
import tempfile
from pathlib import Path
from textwrap import dedent


README_CONTENT = {
    "backend/accounting/README.md": """
# Accounting Module

## Purpose

The Accounting module owns accounting-batch constitution, submission tracking
and reconciliation for completed Payment operations.

## Responsibilities

- select eligible Payment records for accounting;
- build and persist accounting batches and batch items;
- submit batches through the provider-specific accounting adapter;
- reconcile acknowledged, rejected and unknown outcomes;
- expose the internal accounting-batch query API.

Provider-specific DTOs, mappings and OAuth2 client configuration remain inside
Accounting. Provider-neutral HTTP and resilience support belongs to
backend/integration.

## API

Base path: /internal/api/v1/accounting-batches

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /internal/api/v1/accounting-batches | Search accounting batches |
| GET | /internal/api/v1/accounting-batches/{batchId} | Retrieve one batch |

The active contract is:
documentation/contracts/internal/accounting-query-api-v1.yaml

## Persistence

Accounting owns its PostgreSQL tables and Flyway migrations. Repository
adapters and JPA entities remain under the infrastructure.persistence package.
Unknown financial outcomes are reconciled before any retry; blind resubmission
is prohibited.

## Structure

The module follows the Partner reference layering:

- domain: batches, tracking and accounting policies;
- application: constitution, selection and reconciliation use cases;
- api: HTTP controllers, validation and mapping;
- infrastructure: persistence and accounting-provider adapters.

## Validation

From backend:

    mvn -pl accounting -am test
    mvn -pl accounting -am clean verify
    mvn -pl accounting -am -Pfull-tests clean verify

The full-tests command requires Docker for PostgreSQL integration tests.
""",
    "backend/administration/README.md": """
# Administration Module

## Purpose

The Administration module owns administrative HTTP boundaries and operational
queries. It does not own users, identities, roles, permissions or
authentication; those responsibilities belong to Security.

## Responsibilities

- expose Security user-administration commands through an administrative API;
- expose operational overview, settings and integration-health projections;
- search and retrieve operational incidents;
- keep operational concerns separate from Payment audit reporting.

## APIs

Security user administration:

    /internal/api/v1/administration/users

Current operations include create, list, retrieve, update, enable, disable,
delete, local authentication-method management, local password reset, OIDC
identity linking and OIDC identity unlinking.

Operational queries:

    GET /internal/api/v1/administration/overview
    GET /internal/api/v1/administration/settings
    GET /internal/api/v1/administration/integrations

Incident queries:

    GET /internal/api/v1/incidents
    GET /internal/api/v1/incidents/{incidentId}

The active administrative contract is:
documentation/contracts/internal/administration-operational-api-v1.yaml

## Boundaries

- Security remains the owner of canonical users, identities and authorization.
- Reporting remains the owner of immutable Payment audit queries and exports.
- Administration collaborates with other modules through application ports.
- Internal calls remain in-process in the modular monolith.

## Validation

From backend:

    mvn -pl administration -am test
    mvn -pl administration -am clean verify
""",
    "backend/customer/README.md": """
# Customer Module

## Purpose

The Customer module owns customer enrollment and management, local customer
subscription management, current customer verification and ObservedCustomer
read projections.

## Responsibilities

- create, list, retrieve, update, suspend, reactivate and delete customers;
- manage customer bank accounts and the default account;
- create, activate, suspend, retrieve, list and close CustomerSubscription;
- verify customer and account information through the owning banking adapter;
- link and unlink ObservedCustomer records to local customers;
- expose customer observation and customer audit queries.

The external TRESOR PAY subscription remains outside the Payment MVP. It must
not be confused with the local CustomerSubscription capability owned by this
module.

## APIs

Customer management:

    /internal/api/v1/customers

Customer subscriptions:

    /internal/api/v1/subscriptions

Customer observation:

    /internal/api/v1/observed-customers

Customer audit:

    /internal/api/v1/customer-audit-records

Active contracts include:

- documentation/contracts/internal/customer-management-query-api-v1.yaml;
- documentation/contracts/internal/customer-subscription-management-api-v1.yaml;
- documentation/contracts/internal/observed-customer-query-api-v1.yaml.

## Boundaries

- Amplitude-specific verification clients and mappings remain in Customer.
- ObservedCustomer is a read projection, not the canonical banking identity.
- CustomerSubscription is local to Customer and is not a Payment aggregate.
- Security owns authentication and authorization.
- Cross-module collaboration uses application ports and published contracts.

## Structure

Customer follows the Partner reference layering. Its main capability areas
are management, verification and observation, each with explicit api,
application, domain, infrastructure and configuration boundaries.

## Validation

From backend:

    mvn -pl customer -am test
    mvn -pl customer -am clean verify
    mvn -pl customer -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.
""",
    "backend/integration/README.md": """
# Integration Module

## Purpose

The Integration module provides provider-neutral transport, messaging, Outbox
relay, correlation, resilience, Kafka and consumer-idempotency capabilities.
Business rules and provider-specific mappings remain in their owning modules.

## Transport

The active transport is selected by configuration:

| Value | Use |
|---|---|
| internal | In-process communication in the modular monolith |
| kafka | Distributed transport for a future service deployment |

The transport changes only the implementation of IntegrationEventTransport. It
does not change domain events or module-owned Outbox records.

## Reference configuration

    sixpay.messaging.transport: internal
    sixpay.messaging.outbox.enabled: true
    sixpay.messaging.outbox.polling-delay: 1000
    sixpay.messaging.outbox.batch-size: 50
    sixpay.messaging.outbox.max-attempts: 5
    sixpay.messaging.outbox.retry-delay: 30s
    sixpay.messaging.outbox.processing-timeout: 5m

Kafka-specific properties remain under spring.kafka and the sixpay.messaging.kafka
namespace.

## Guarantees

- aggregate state and the module Outbox record share one transaction;
- claims use PostgreSQL row locking with SKIP LOCKED;
- retryable failures are recorded with a next attempt time;
- exhausted deliveries move to DEAD;
- abandoned PROCESSING claims become eligible again after the timeout;
- delivery is at least once, so consumers must be idempotent.

## Boundaries

Integration is not an omnipotent domain service. It does not own Partner,
Customer, Payment, Accounting or Notification business decisions. Provider
payloads and mappings stay in the owning domain.

## Validation

From backend:

    mvn -pl integration -am test
    mvn -pl integration -am clean verify
    mvn -pl integration -am -Pfull-tests clean verify

The full-tests command requires Docker for PostgreSQL-backed integration tests.
""",
    "backend/notification/README.md": """
# Notification Module

## Purpose

The Notification module owns delivery of Partner decision notifications and
operational notifications.

## Responsibilities

Partner decision notifications:

- consume integration events through Notification-owned adapters;
- select the supported notification template;
- deliver the notification without calling Partner directly.

Operational notifications:

- model notification requests and delivery state;
- schedule, retry, replay and retain operational deliveries;
- persist delivery state and operational metrics;
- deliver through configured SMTP or other supported channels.

## Boundaries

- Notification does not decide Partner, Payment or Accounting business state.
- Integration provides transport and event delivery support.
- Notification owns templates, routing and delivery lifecycle.
- Delivery is at least once and must be handled idempotently.

## Validation

From backend:

    mvn -pl notification -am test
    mvn -pl notification -am clean verify
    mvn -pl notification -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.
""",
    "backend/partner/README.md": """
# Partner Module

## Purpose

Partner is the reference business module for SIXPAY CONNECT. It demonstrates
the repository conventions for domain modeling, application ports, secure HTTP
APIs, persistence, Outbox events and layered tests.

## Responsibilities

- create, validate, reject, suspend and reactivate partners;
- configure validation thresholds;
- expose partner catalog, status and audit queries;
- persist immutable audit and threshold history;
- publish versioned Partner integration events through its Outbox.

## API

Base path: /api/v1/partners

The API supports partner creation, listing, retrieval, validation, suspension,
reactivation, threshold configuration, status and audit queries. Mutating
requests use correlation and idempotency controls.

## Structure

    api/
    application/
    domain/
    infrastructure/
    configuration/
    events/

The module is non-executable. Bootstrap assembles it through its auto-
configuration; Partner itself does not depend directly on Notification,
Payment or the transport implementation.

## Boundaries

- Security provides the authenticated principal and authorities.
- Partner owns Partner business rules, persistence and event production.
- Integration relays Outbox messages using the configured transport.
- Consumers receive versioned events and do not call Partner to reconstruct
  the event decision.

## Persistence and reliability

Partner owns its Flyway migration, audit tables, threshold history, Outbox
records and idempotency store. Invalid transitions are rejected in the domain.
Audit and history are immutable, and concurrent mutations are protected by
database constraints and transactional idempotency.

## Validation

From backend:

    mvn -pl partner -am clean verify
    mvn -pl partner -am -Pfull-tests clean verify
    mvn -pl partner -am -Pcoverage clean verify

The full-tests command requires Docker for PostgreSQL integration tests.
""",
    "backend/payment/README.md": """
# Payment Module

## Purpose

The Payment module owns payment business behavior, state transitions,
idempotency, audit and Outbox boundaries.

## Responsibilities

- accept and validate payment commands;
- coordinate customer, account and banking verification results;
- enforce Payment invariants and legal state transitions;
- persist payment state, audit records and Outbox records atomically;
- expose Payment query and timeline capabilities;
- reconcile external outcomes without blind financial replay.

## API

The module exposes the Payment query endpoints under:

    /internal/api/v1/payments

Payment audit timeline and audit export endpoints are owned by Reporting and
are documented by the corresponding internal contracts.

## Boundaries

- Integration owns provider-neutral transport only.
- Customer owns customer verification and CustomerSubscription.
- Accounting owns accounting batches and reconciliation.
- Reporting owns immutable Payment audit queries and exports.
- Payment does not manage external TRESOR PAY subscriptions.

## Structure

The module follows the Partner reference layering:

- api;
- application;
- domain;
- infrastructure;
- configuration;
- events.

## Validation

From backend:

    mvn -pl payment -am test
    mvn -pl payment -am clean verify
    mvn -pl payment -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.
""",
    "backend/reporting/README.md": """
# Reporting Module

## Purpose

The Reporting module owns immutable Payment audit queries and controlled audit
exports. It is a read-oriented module and does not own Payment state
transitions.

## APIs

    GET /internal/api/v1/payments/{paymentId}/timeline
    GET /internal/api/v1/payment-audit-records
    GET /internal/api/v1/payment-audit-records/{auditId}
    POST /internal/api/v1/payment-audit-exports
    GET /internal/api/v1/payment-audit-exports/{exportId}

Read operations require payment audit read authority. Export operations also
require payment audit export authority.

## Responsibilities

- query masked Payment audit records and timelines;
- apply authenticated cursors and access checks;
- record audit-access activity;
- create idempotent export jobs;
- generate and store controlled export artifacts.

## Boundaries

- Payment remains the owner of Payment state and financial transitions.
- Reporting never initiates posting, reversal or other financial commands.
- Operational incident querying belongs to Administration.
- Persistence adapters and export stores remain inside Reporting infrastructure.

## Validation

From backend:

    mvn -pl reporting -am test
    mvn -pl reporting -am clean verify
    mvn -pl reporting -am -Pfull-tests clean verify

The full-tests command requires Docker when PostgreSQL integration tests are
selected.
""",
    "backend/security/README.md": """
# Security Module

## Purpose

The Security module provides shared authentication, authorization, identity
linking, password lifecycle and security-audit capabilities.

## Capabilities

- local authentication and session management;
- OIDC session integration;
- JWT resource-server authority conversion;
- SIXPAY-owned roles and permissions;
- local password change and reset support;
- user-account and external-identity linking;
- authentication and security operational audit.

The identity provider proves identity. SIXPAY owns authorization and maps the
authenticated identity to SIXPAY roles and permissions.

## API

Authentication and session endpoints:

    /api/v1/auth/login
    /api/v1/auth/me
    /api/v1/auth/session/oidc
    /api/v1/auth/logout
    /api/v1/auth/password/change

Administration exposes user-management HTTP boundaries while Security owns the
underlying users, identities, credentials and authorization data.

## Boundaries

- Security does not own business-domain aggregates.
- Administration calls Security application capabilities through ports.
- Business modules consume the authenticated principal and authorities.
- Secrets and provider credentials are supplied by runtime configuration.

## Validation

From backend:

    mvn -pl security -am test
    mvn -pl security -am clean verify
    mvn -pl security -am -Pfull-tests clean verify

The full-tests command requires Docker when integration tests are selected.
""",
    "backend/tests/README.md": """
# Backend Test Foundation

## Purpose

The tests module hosts tests that require multiple bounded contexts or the
assembled application. Module-local tests remain beside the implementation
they verify.

## Responsibilities

- assembled Spring application-context verification;
- contract-backed cross-module integration tests;
- full-stack persistence and security integration tests;
- repository-level coverage and architecture gates.

## Ownership rule

Domain, application, API and persistence behavior is tested in the owning
module. The tests module verifies only cross-module or assembled behavior.

## Execution

From backend:

    mvn -pl tests test
    mvn -pl tests -Pfull-tests verify

Repository-wide verification:

    python scripts/verify_baseline.py
""",
}


CUSTOMER_OBSERVATION_README = """
# Customer Observation

## Purpose

This directory contains implementation notes and operational evidence for the
Customer observation capability. The current implementation is owned by the
Customer module.

## Scope

Customer observation provides read-oriented ObservedCustomer projections,
search, detail, payment history and controlled links between an observed
banking identity and a local Customer.

ObservedCustomer is not the canonical banking identity and does not replace
fresh verification against the banking provider.

## Ownership boundaries

- Customer owns observation queries, projections and links.
- Customer verification owns provider interaction and mapping.
- Payment consumes the defined customer verification result but does not own
  the observation projection.
- Security owns authentication and authorization.

## Validation

Run the Customer module tests from backend:

    mvn -pl customer -am test
    mvn -pl customer -am clean verify
"""


def write_if_changed(path: Path, value: str) -> bool:
    normalized = dedent(value).strip() + "\n"
    if path.read_text(encoding="utf-8") == normalized:
        return False
    path.write_text(normalized, encoding="utf-8", newline="")
    return True


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("repository", nargs="?", default=".",
                        help="repository root (default: current directory)")
    root = Path(parser.parse_args().repository).resolve()
    targets = {root / relative: content for relative, content in README_CONTENT.items()}
    targets[root / "documentation/implementation/customer-observation/README.md"] = CUSTOMER_OBSERVATION_README

    missing = [str(path.relative_to(root)) for path in targets if not path.is_file()]
    if missing:
        raise SystemExit("Missing README files:\n- " + "\n- ".join(missing))

    backup_dir = Path(tempfile.mkdtemp(prefix="sixpay-readme-cleanup-"))
    changed = []
    for path, content in targets.items():
        shutil.copy2(path, backup_dir / (path.name + "." + str(len(changed))))
        if write_if_changed(path, content):
            changed.append(str(path.relative_to(root)))

    leftovers = []
    for path in targets:
        text = path.read_text(encoding="utf-8")
        if any(token in text for token in ("Phase ", "FS-", "DA-", "IA-", "LOT_")):
            leftovers.append(str(path.relative_to(root)))
    if leftovers:
        raise RuntimeError("Phase/lot references remain in: " + ", ".join(leftovers))

    print("Module README cleanup applied.")
    for relative in changed:
        print(" - " + relative)
    if not changed:
        print(" - no changes (already aligned)")
    print("Temporary backups: " + str(backup_dir))
    print("No commit or push was performed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
