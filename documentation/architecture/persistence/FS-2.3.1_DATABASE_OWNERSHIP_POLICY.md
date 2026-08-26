# FS-2.3.1 — Database Ownership & Flyway Policy

**Branch:** `feat/repository-baseline-consolidation-cleanup`
**Gate:** `FS-2.3 — Database baseline consolidation`
**Status:** Policy
**Golden module:** Partner

## Purpose

This document defines the canonical persistence ownership and Flyway migration
policy for SIXPAY CONNECT after the FS-2.3 baseline consolidation.

It is normative for all backend modules.

The policy is derived from:

- the modular-monolith architecture;
- Partner as the golden business-module reference;
- the FS-2.3.0 migration inventory;
- the target capability ownership already established in the repository;
- the requirement to preserve future microservice extractability.

## 1. Core ownership rule

The module that owns a business model owns:

1. its persistence entities and repositories;
2. its database tables and database-specific constraints;
3. its indexes, triggers and database functions;
4. its Flyway migrations;
5. the lifecycle of those database objects.

Canonical rule:

```text
domain owns model
    ↓
domain owns persistence
    ↓
domain owns tables
    ↓
domain owns Flyway migrations
```

A migration MUST live under the module that owns the database objects it
defines or evolves.

Canonical location:

```text
backend/<domain>/src/main/resources/db/migration/
```

Examples:

```text
backend/partner/src/main/resources/db/migration/
backend/payment/src/main/resources/db/migration/
backend/customer/src/main/resources/db/migration/
```

## 2. Bootstrap responsibility

`backend/bootstrap` is the application assembler.

Bootstrap is responsible for:

- assembling the modular monolith;
- starting Spring Boot;
- composing module configuration;
- providing runtime infrastructure configuration;
- executing Flyway over migrations available on the assembled classpath.

Bootstrap is **not** the owner of business-domain tables.

Normative rule:

```text
business migration in bootstrap = FORBIDDEN
```

Therefore:

```text
backend/bootstrap/src/main/resources/db/migration/
```

MUST contain no business-domain migration after FS-2.3 consolidation.

A migration MUST NOT be placed in Bootstrap merely because:

- several modules are packaged in one application;
- all modules share one PostgreSQL database;
- Flyway is launched by Bootstrap;
- the migration contains identifiers belonging to more than one domain.

Runtime execution ownership and business schema ownership are separate
concerns:

```text
Flyway execution owner   = assembled application / Bootstrap runtime
Migration definition owner = business module
```

## 3. Flyway version ranges

All module migrations are visible to the same Flyway runtime today.

Flyway versions therefore MUST be globally unique across the assembled
classpath.

The following version ranges are reserved:

| Module | Baseline | Reserved range |
|---|---|---:|
| Partner | `V100__partner_baseline.sql` | 100–199 |
| Customer | `V200__customer_baseline.sql` | 200–299 |
| Payment | `V300__payment_baseline.sql` | 300–399 |
| Accounting | `V400__accounting_baseline.sql` | 400–499 |
| Reporting | `V500__reporting_baseline.sql` | 500–599 |
| Notification | `V600__notification_baseline.sql` | 600–699 |
| Security | `V700__security_baseline.sql` | 700–799 |
| Administration | `V800__administration_baseline.sql` | 800–899 |
| Platform / Bootstrap exceptional | none by default | 900–999 |

Examples after baseline:

```text
Partner:
V100__partner_baseline.sql
V101__add_partner_x.sql
V102__add_partner_y.sql

Payment:
V300__payment_baseline.sql
V301__add_payment_x.sql
V302__add_payment_y.sql
```

A module MUST NOT use another module's reserved range.

## 4. Platform range policy

The `900–999` range is exceptional.

It MUST NOT become a generic location for migrations that are difficult to
classify.

A platform migration is allowed only when all of the following are true:

1. the database object is genuinely platform-owned;
2. no business domain owns its lifecycle;
3. the object is required by the assembled platform itself;
4. ownership is documented explicitly;
5. the migration does not introduce hidden business coupling.

Default rule:

```text
Platform migration = NONE
```

The FS-2.3.0 inventory found no existing unique Bootstrap-owned platform schema.

## 5. Single database / single logical schema

SIXPAY CONNECT remains, for the current modular-monolith baseline:

```text
one PostgreSQL database
one logical SIXPAY schema
multiple domain-owned table sets
```

FS-2.3 does **not** introduce one PostgreSQL schema per business domain.

Domain ownership is enforced through:

- module boundaries;
- migration ownership;
- repository ownership;
- absence of forbidden cross-domain SQL dependencies;
- architectural tests and gates.

Physical schema separation can be reconsidered during an actual microservice
extraction.

## 6. Repository ownership rule

A module MUST access its own tables through its own persistence adapters.

Forbidden:

```text
Payment repository → Customer table
Customer repository → Partner table
Accounting repository → Payment table
```

Allowed:

```text
Payment use case
    ↓ port
Customer capability / projection / event / API
```

or another explicitly designed module interaction.

The fact that tables currently reside in the same PostgreSQL database MUST NOT
be used to bypass module boundaries.

## 7. Foreign-key policy

### 7.1 Same-domain foreign keys

Foreign keys between tables owned by the same module are allowed and encouraged
when they enforce valid domain persistence invariants.

Examples:

```text
payment_audit.payment_id
    → payments.payment_id

accounting_batch_tracking.batch_id
    → accounting_batches.id

customer_management_bank_account.customer_id
    → customer_management_customer.customer_id
```

### 7.2 Cross-domain foreign keys

A physical SQL foreign key crossing bounded-context ownership is forbidden by
default.

Canonical rule:

```text
same owner:
    SQL FK allowed

different owner:
    SQL FK forbidden by default
```

Cross-domain relationships SHOULD be represented by stable logical identifiers:

```text
partner_id
payment_id
observed_customer_id
accounting_batch_id
```

without a physical SQL FK to another domain's table.

This protects future microservice extraction.

### 7.3 Exception process

A cross-domain FK may exist only if:

1. ownership analysis proves both tables are actually owned by the same bounded
   context; or
2. an explicit architecture decision record justifies the coupling.

No implicit exception is allowed.

## 8. Logical references

A column carrying another domain's identifier does not transfer ownership.

Examples from the current baseline inventory:

```text
Customer Subscription.partner_id
Accounting Batch Item.payment_id
Reporting Evidence.payment_id
Reporting Evidence.observed_customer_id
Administration Incident.payment_id
Administration Incident.accounting_batch_id
```

These are valid logical references when no cross-domain FK or direct repository
access is introduced.

## 9. Baseline squash policy

Because no production database exists yet, the pre-production development
history is not part of the canonical runtime baseline.

The final baseline MUST describe the current schema directly.

Historical migration sequences such as:

```text
CREATE
ALTER
ALTER
UPDATE migration data
DROP temporary object
ADD final constraint
```

SHOULD be collapsed into the final `Vx00__<domain>_baseline.sql`.

Git preserves the development history.

Canonical baseline rule:

```text
Flyway baseline = current required schema state
Git history      = how that state was reached
```

A `DELETE_HISTORY` decision from FS-2.3.0 means:

```text
preserve final effect
remove historical migration step
```

It never means removing the resulting schema requirement.

## 10. Baseline immutability after release

The squash privilege exists only because SIXPAY CONNECT has no production
database yet.

Once a baseline has been deployed to an environment whose Flyway history must
be preserved, an applied migration MUST NOT be edited, renamed, reordered or
deleted.

After the canonical baseline is released:

```text
V300 applied
    ↓
future Payment change = V301
```

not:

```text
edit V300
```

This rule protects Flyway checksum integrity and repeatable deployment.

## 11. Module extraction rule

A domain's migrations MUST be sufficiently self-contained that the module can
later be extracted into a microservice without reconstructing its schema from
Bootstrap-owned SQL.

Target property:

```text
copy module
+ copy its migration range
+ configure its database
= domain schema can be created
```

This does not require a separate database today; it requires correct ownership
today.

## 12. Current ownership mapping

FS-2.3.0 establishes the following target ownership:

| Objects | Owner |
|---|---|
| Partner tables / outbox / audit / idempotency | Partner |
| Customer Management + Observed Customer tables | Customer |
| Payment aggregate / audit / outbox / idempotency | Payment |
| Accounting batch + tracking tables | Accounting |
| Reporting audit projection + export tables | Reporting |
| Notification delivery / operational notification tables | Notification |
| Security account / identity / role / permission / credential / audit tables | Security |
| Operational incident + timeline | Administration |

Bootstrap owns none of these tables.

## 13. `payment_observed_customer_link`

FS-2.3.0 leaves this object under explicit review.

Current structure:

```text
payment_observed_customer_link
├── payment_id            PK + FK → payments.payment_id
└── observed_customer_id  logical reference only
```

Provisional owner:

```text
Payment
```

Reason:

- the primary key is `payment_id`;
- the only SQL FK points to Payment;
- lifecycle deletion cascades from Payment;
- no Customer table is referenced physically.

The final ownership decision remains part of the dedicated cross-domain review
before V300 is generated.

Until that decision is closed, no new cross-domain FK may be added to this
table.

## 14. Migration naming policy

After FS-2.3 baseline creation, migrations MUST use:

```text
V<module-range-version>__<lower_snake_case_description>.sql
```

Examples:

```text
V100__partner_baseline.sql
V201__add_customer_search_index.sql
V302__add_payment_reconciliation_marker.sql
V701__add_security_authentication_policy.sql
```

Descriptions SHOULD describe the resulting database change, not the ticket or
development phase that produced it.

Avoid names such as:

```text
fix_again
patch
lot_3
da10
temporary_fix
schema_v2_fix
```

unless the term is itself part of the permanent business concept.

## 15. Non-regression gates required by FS-2.3

The final FS-2.3 gate MUST eventually reject:

- business-domain SQL under Bootstrap;
- duplicate Flyway versions;
- migration versions outside the owner's reserved range;
- duplicate migration files;
- new unapproved cross-domain foreign keys;
- restoration of pre-baseline development-history migrations;
- domain persistence entities whose tables have no owning module migration.

The implementation of these gates is handled by later FS-2.3 steps.

## 16. Policy summary

Canonical persistence rules:

```text
1. Module owns its model.
2. Module owns its repositories.
3. Module owns its tables.
4. Module owns its Flyway migrations.
5. Bootstrap assembles; Bootstrap does not own business schema.
6. Flyway versions are globally unique.
7. Each module stays inside its reserved range.
8. Same-domain FK is allowed.
9. Cross-domain FK is forbidden by default.
10. Cross-domain identifiers remain logical references.
11. One PostgreSQL database/schema remains sufficient for the modular monolith.
12. Development migration history is squashed before production.
13. Applied production migrations become immutable.
14. Git preserves development history.
15. Migration ownership must support future service extraction.
```

## FS-2.3.1 exit criteria

FS-2.3.1 is complete when:

- the module version ranges are formally documented;
- business migrations in Bootstrap are formally forbidden;
- the distinction between Flyway execution and migration ownership is explicit;
- the cross-domain FK policy is explicit;
- the single-database/single-logical-schema decision is explicit;
- the pre-production squash rule is explicit;
- post-release Flyway immutability is explicit;
- the policy is consistent with FS-2.3.0;
- no SQL migration has yet been modified.

## Decision

The canonical SIXPAY CONNECT persistence ownership model is:

```text
backend/bootstrap
    = assembler / Flyway runtime

backend/<domain>
    = persistence owner
    = table owner
    = migration owner
```

and:

```text
business migration in bootstrap = FORBIDDEN
```
