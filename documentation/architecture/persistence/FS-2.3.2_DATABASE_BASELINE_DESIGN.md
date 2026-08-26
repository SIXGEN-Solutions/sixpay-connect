# FS-2.3.2 — Database Baseline Design Matrix

**Branch:** `feat/repository-baseline-consolidation-cleanup`
**Gate:** `FS-2.3 — Database baseline consolidation`
**Status:** Design / no SQL deletion yet
**Golden module:** Partner

## Purpose

This document defines the exact target composition and creation order of the
canonical Flyway baselines before any historical migration is deleted.

The design must satisfy both:

1. the FS-2.3.1 ownership/range policy;
2. SQL dependency ordering inside each baseline.

No migration is removed by FS-2.3.2.

## Global execution order

The assembled modular monolith exposes all module migrations to one Flyway
runtime. The global execution order is therefore:

```text
V100 Partner
  ↓
V200 Customer
  ↓
V300 Payment
  ↓
V400 Accounting
  ↓
V500 Reporting
  ↓
V600 Notification
  ↓
V700 Security
  ↓
V800 Administration
```

The design deliberately avoids any FK that requires a later module baseline.

## FK ordering rule

A foreign key created in a baseline must point to:

- a table created earlier in the same baseline; or
- exceptionally, an earlier baseline owned by the same architectural owner.

Current target design uses only same-domain FK graphs.

Cross-domain identifiers remain logical values without SQL FKs.

---

# V100 — Partner baseline

**Target file**

```text
backend/partner/src/main/resources/db/migration/
V100__partner_baseline.sql
```

## Internal creation order

1. `partners`
2. indexes on `partners`
3. `partner_authorized_perimeters`
4. `partner_validation_thresholds`
5. `partner_validation_threshold_history`
6. history indexes
7. `partner_audit`
8. audit indexes
9. append-only function `partner_reject_immutable_change`
10. immutable triggers
11. `partner_idempotency`
12. idempotency indexes
13. `partner_outbox_events` in its **final industrialized shape**
14. final outbox indexes

## FK graph

```text
partners
├── partner_authorized_perimeters
├── partner_validation_thresholds
├── partner_validation_threshold_history
├── partner_audit
└── partner_idempotency

partner_authorized_perimeters
└── partner_validation_thresholds
```

No external FK.

## Historical effects to fold into V100

`V2026072701__industrialize_partner_outbox.sql` must be absorbed directly into
the final `partner_outbox_events` definition. No post-create ALTER should remain
in V100 for fields that are already part of the final baseline.

---

# V200 — Customer baseline

**Target file**

```text
backend/customer/src/main/resources/db/migration/
V200__customer_baseline.sql
```

## Internal creation order

### Observed Customer projection

1. `customer_observed_customer`
2. `customer_observed_institution`
3. `customer_observed_account`
4. `customer_observed_payment`
5. `customer_observation_processed_event`
6. observed-customer query/keyset/snapshot indexes
7. `customer_observation_audit`
8. append-only audit function/trigger if currently required

### Authoritative Customer Management

9. `customer_management_customer`
10. customer search indexes
11. `customer_management_bank_account`
12. `customer_management_subscription`
13. `customer_observed_master_link`
14. `customer_management_audit`
15. management audit/search indexes

## FK graph

```text
customer_observed_customer
├── customer_observed_institution
├── customer_observed_account
├── customer_observed_payment
└── customer_observed_master_link

customer_management_customer
├── customer_management_bank_account
├── customer_management_subscription
└── customer_observed_master_link

customer_management_bank_account
└── customer_management_subscription
```

## Cross-domain logical references

```text
customer_management_subscription.partner_id
customer_observed_payment.payment_id
customer_observation_audit.payment_id
```

These remain logical identifiers only.

No Partner or Payment FK is allowed in V200.

## Historical effects to fold into V200

- query indexes from `V20260804.01`;
- final search indexes from `V20260822.05`.

Those files disappear after squash, but their final indexes remain in V200.

---

# V300 — Payment baseline

**Target file**

```text
backend/payment/src/main/resources/db/migration/
V300__payment_baseline.sql
```

## Internal creation order

1. `payments` with final state-schema constraint
2. core Payment indexes
3. `payment_audit`
4. audit append-only function/triggers
5. audit indexes
6. `payment_outbox_events`
7. outbox indexes
8. `payment_idempotency`
9. idempotency indexes
10. `payment_observed_customer_link`

## FK graph

```text
payments
├── payment_audit
├── payment_outbox_events
├── payment_idempotency
└── payment_observed_customer_link
```

## Cross-domain logical reference

```text
payment_observed_customer_link.observed_customer_id
```

must remain a UUID/reference with **no FK to Customer**.

## Historical effects to fold into V300

`V2026080105__payment_state_schema_v2.sql` is absorbed directly into the initial
`payments` constraint. V300 must create the final accepted state-schema version
set from the start.

## Ownership decision

`payment_observed_customer_link` is Payment-owned. Its lifecycle and only SQL FK are anchored to `payments`; `observed_customer_id` remains a logical Customer reference without a cross-domain FK.

---

# V400 — Accounting baseline

**Target file**

```text
backend/accounting/src/main/resources/db/migration/
V400__accounting_baseline.sql
```

## Internal creation order

1. `accounting_batches`
2. batch indexes
3. `accounting_batch_items`
4. batch-item indexes
5. `accounting_batch_tracking`
6. `accounting_batch_item_tracking`
7. tracking indexes

## FK graph

```text
accounting_batches
├── accounting_batch_items
└── accounting_batch_tracking

accounting_batch_items
└── accounting_batch_item_tracking
```

No Payment or Partner FK.

## Cross-domain logical references

```text
accounting_batch_items.payment_id
accounting_batch_items.partner_id
```

These remain values only.

The two current Bootstrap migrations are moved and merged into V400.

---

# V500 — Reporting baseline

**Target file**

```text
backend/reporting/src/main/resources/db/migration/
V500__reporting_baseline.sql
```

## Internal creation order

1. `reporting_payment_audit_evidence` in final shape, including
   `financial_institution_code`
2. evidence indexes
3. `reporting_payment_audit_export_job`
4. export indexes / constraints

## FK graph

No cross-domain SQL FK.

## Cross-domain logical references

```text
reporting_payment_audit_evidence.payment_id
reporting_payment_audit_evidence.observed_customer_id
```

These remain projection identifiers only.

## Historical effects to fold into V500

The later ALTER that adds `financial_institution_code` is represented directly
in the table's baseline definition.

---

# V600 — Notification baseline

**Target file**

```text
backend/notification/src/main/resources/db/migration/
V600__notification_baseline.sql
```

## Internal creation order

### Notification delivery

1. `notification_deliveries` in final shape, including `reason`
2. delivery indexes

### Operational Notification

3. `sixpay.operational_notification_deliveries` in final shape
4. operational-delivery indexes
5. `sixpay.operational_notification_attempts`
6. attempts indexes
7. `sixpay.operational_notification_replays`
8. replay indexes / constraints

## FK graph

```text
operational_notification_deliveries
├── operational_notification_attempts
└── operational_notification_replays
```

No cross-domain FK.

## Historical effects to fold into V600

- `notification_deliveries.reason` exists in the initial table definition;
- replay/retry fields exist in the initial operational-delivery definition;
- the two duplicate Bootstrap files are deleted during squash.

---

# V700 — Security baseline

**Target file**

```text
backend/security/src/main/resources/db/migration/
V700__security_baseline.sql
```

Security requires the most deliberate redesign because current migrations
represent a transition from temporary Local-auth-owned authorization toward a
canonical SIXPAY user model.

V700 must create the **final model directly**.

## Internal creation order

1. `security_user_accounts`
2. account indexes/constraints
3. `security_user_identities`
4. identity indexes/constraints
5. `security_local_users` as the final Local credential store linked directly
   to `security_user_accounts`
6. Local credential indexes and final password-lifecycle constraints
7. `security_user_roles`
8. `security_user_permissions`
9. `security_password_history`
10. `security_authentication_audit`
11. authentication-audit indexes
12. `security_audit_events` with its **final complete event-type constraint**
13. audit indexes
14. append-only audit functions/triggers

## FK graph

```text
security_user_accounts
├── security_user_identities
├── security_local_users
├── security_user_roles
├── security_user_permissions
├── security_password_history
└── security_audit_events.target_user_id (optional)
```

All SQL FKs remain Security-local.

## Objects that MUST NOT exist in the baseline

Temporary development structures must not be recreated merely to migrate them:

```text
security_local_user_authorities
```

The current migration history creates this table, moves its values to canonical
roles/permissions, then drops it. V700 starts directly with the canonical model.

## Historical effects to fold into V700

- identity linking;
- local credential → canonical user linking;
- canonical roles/permissions;
- final password lifecycle columns;
- password history;
- final audit-event type set including later DA-10 values.

No migration-time INSERT/UPDATE should be needed to transform data from a
pre-baseline schema because the target database starts empty.

---

# V800 — Administration baseline

**Target file**

```text
backend/administration/src/main/resources/db/migration/
V800__administration_baseline.sql
```

## Internal creation order

1. `operational_incident`
2. incident indexes
3. `operational_incident_timeline`
4. timeline indexes

## FK graph

```text
operational_incident
└── operational_incident_timeline
```

## Cross-domain logical references

```text
operational_incident.accounting_batch_id
operational_incident.payment_id
operational_incident.payment_reference
```

No FK to Accounting or Payment.

The current Administration migration is already close to baseline shape and can
be retained semantically while renumbering.

---

# Global FK-order verification

The resulting baseline dependency graph is:

```text
V100 Partner
  └── Partner-local FK only

V200 Customer
  └── Customer-local FK only

V300 Payment
  └── Payment-local FK only

V400 Accounting
  └── Accounting-local FK only

V500 Reporting
  └── no external FK

V600 Notification
  └── Notification-local FK only

V700 Security
  └── Security-local FK only

V800 Administration
  └── Administration-local FK only
```

Therefore:

```text
No Vx00 baseline depends on a table created in a later module range.
```

This is the required Flyway ordering invariant.

# Baseline composition matrix

| Baseline | Owner | Main tables / objects | External physical FK | Design status |
|---|---|---|---|---|
| V100 | Partner | partner aggregate, perimeter, thresholds/history, audit, idempotency, outbox | None | READY |
| V200 | Customer | observed-customer projection/audit + customer management/account/subscription/link/audit | None | READY |
| V300 | Payment | payments, audit, outbox, idempotency, observed-customer link | None to Customer | READY |
| V400 | Accounting | batches, items, batch/item tracking | None | READY |
| V500 | Reporting | audit evidence, audit export job | None | READY |
| V600 | Notification | delivery + operational delivery/attempt/replay | None | READY |
| V700 | Security | accounts, identities, Local credentials, roles, permissions, password history, auth/audit | None | READY |
| V800 | Administration | operational incident + timeline | None | READY |

# Objects intentionally absent from final baselines

The following development-history structures/steps must not survive as baseline
artifacts:

- temporary `security_local_user_authorities`;
- post-create Payment state-schema ALTER;
- Partner outbox industrialization ALTER sequence;
- Customer index-only migrations;
- Notification retry-payload ALTER;
- Security lifecycle/audit constraint widening migrations;
- Bootstrap duplicates of Notification migrations.

Their **final effects** remain in the relevant Vx00 baseline.

# FS-2.3.2 exit criteria

FS-2.3.2 is complete when:

- V100–V800 target contents are documented;
- every table belongs to exactly one target baseline;
- every durable index/constraint/function/trigger is assigned;
- every same-domain FK is ordered after its referenced table;
- no baseline has a dependency on a later module range;
- no unauthorized cross-domain FK is introduced;
- historical ALTER/fix/index-only effects are assigned to final table
  definitions;
- temporary Security migration structures are excluded from the final schema;
- `payment_observed_customer_link` is classified as Payment-owned and keeps `observed_customer_id` as a logical reference only;
- no SQL migration has yet been deleted.

# Decision

The target baseline design is structurally valid for one Flyway runtime because
each Vx00 baseline is internally self-contained and no module baseline requires
a later module's tables.

FS-2.3.3 may perform the squash with `payment_observed_customer_link` included in V300 as Payment-owned persistence.
