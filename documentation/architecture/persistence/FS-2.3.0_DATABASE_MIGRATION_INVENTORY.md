# FS-2.3.0 — Database Migration Inventory & Ownership Matrix

**Branch:** `feat/repository-baseline-consolidation-cleanup`
**Gate:** `FS-2.3 — Database baseline consolidation`
**Status:** Inventory / no SQL modification
**Golden module:** Partner

## Purpose

This document freezes the current Flyway migration inventory before any
baseline squash.

FS-2.3.0 does **not** modify the database schema and does **not** remove any
migration. It classifies each current migration by:

- current physical owner;
- tables or schema objects touched;
- SQL foreign-key/reference relationships;
- target business owner;
- consolidation decision;
- target baseline.

The inventory is the mandatory input to FS-2.3.1 and FS-2.3.2.

## Decision vocabulary

| Decision | Meaning |
|---|---|
| `MERGE_IN_BASELINE` | The migration contains durable schema content that must be folded into the target owner's baseline. |
| `MOVE_AND_MERGE` | Durable schema content is currently stored under the wrong module and must move into the target owner's baseline. |
| `DELETE_DUPLICATE` | The file is a physical duplicate of an already owner-local migration and must disappear from the final baseline. |
| `KEEP_AS_BASELINE_CONTENT` | The migration is already owner-local and sufficiently baseline-shaped; its schema content may be retained essentially as-is while renumbering. |
| `REVIEW_CROSS_DOMAIN` | Ownership or SQL dependency crosses a bounded context and must be explicitly decided before the squash. |
| `DELETE_HISTORY` | The file only represents an evolution step (`ALTER`, index-only change, constraint widening, migration DML, etc.). Its final effect must be folded into the target baseline and the historical file removed. |

`DELETE_HISTORY` never means "lose the effect". It means "keep the final state,
delete the development-history migration".

## Target version ranges

| Module | Baseline | Reserved range |
|---|---:|---:|
| Partner | `V100__partner_baseline.sql` | 100–199 |
| Customer | `V200__customer_baseline.sql` | 200–299 |
| Payment | `V300__payment_baseline.sql` | 300–399 |
| Accounting | `V400__accounting_baseline.sql` | 400–499 |
| Reporting | `V500__reporting_baseline.sql` | 500–599 |
| Notification | `V600__notification_baseline.sql` | 600–699 |
| Security | `V700__security_baseline.sql` | 700–799 |
| Administration | `V800__administration_baseline.sql` | 800–899 |
| Bootstrap/platform exceptional | — | 900–999 |

## Inventory summary

| Current owner | Migration count | Target state |
|---|---:|---|
| Partner | 2 | Squash to V100 |
| Customer | 8 | Squash to V200 |
| Payment | 5 | Squash to V300 |
| Notification | 4 | Squash to V600 |
| Administration | 1 | Normalize to V800 |
| Bootstrap | 15 | Business-owned migrations evacuated |
| **Total** | **35** | **8 domain baselines + zero business migration in bootstrap** |

Bootstrap currently contains:

- 2 Accounting migrations;
- 2 duplicate Notification migrations;
- 1 Payment/Observed-Customer link migration;
- 2 Reporting migrations;
- 8 Security migrations.

## Detailed ownership matrix

### Partner — target V100

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V2026072601__create_partner_module.sql` | Partner | `partners`, `partner_authorized_perimeters`, `partner_validation_thresholds`, `partner_validation_threshold_history`, `partner_audit`, `partner_idempotency`, `partner_outbox_events`, append-only triggers/function | Only Partner-local FKs to `partners` / perimeter tables | Partner | `MERGE_IN_BASELINE` | V100 |
| `V2026072701__industrialize_partner_outbox.sql` | Partner | Alters `partner_outbox_events`: schema/correlation/retry/claim columns, status constraint, claimable index | None added outside Partner | Partner | `DELETE_HISTORY` | V100 |

### Customer — target V200

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V20260803_01__create_customer_observed_projection.sql` | Customer | `customer_observed_customer`, `customer_observed_institution`, `customer_observed_account`, `customer_observed_payment`, `customer_observation_processed_event` | Customer-local FK graph only; `payment_id` is data, not FK to Payment | Customer | `MERGE_IN_BASELINE` | V200 |
| `V20260804.01__add_observed_customer_query_indexes.sql` | Customer | Query/keyset/snapshot indexes over observed-customer tables | No FK | Customer | `DELETE_HISTORY` | V200 |
| `V20260805.01__create_customer_observation_audit.sql` | Customer | `customer_observation_audit`, append-only trigger/function | `observed_customer_id`, `payment_id`, `source_event_id` are logical references; no SQL FK | Customer | `MERGE_IN_BASELINE` | V200 |
| `V20260822.01__create_customer_management.sql` | Customer | `customer_management_customer`, `customer_management_bank_account` | Bank account → Customer only | Customer | `MERGE_IN_BASELINE` | V200 |
| `V20260822.02__create_customer_subscription.sql` | Customer | `customer_management_subscription` | FK → Customer + Bank Account; `partner_id` deliberately has no FK to Partner | Customer | `MERGE_IN_BASELINE` | V200 |
| `V20260822.03__link_observed_customer_to_customer.sql` | Customer | `customer_observed_master_link` | FK to observed-customer + authoritative customer; both Customer-owned | Customer | `MERGE_IN_BASELINE` | V200 |
| `V20260822.04__create_customer_management_audit.sql` | Customer | `customer_management_audit` | Aggregate id is logical; no FK | Customer | `MERGE_IN_BASELINE` | V200 |
| `V20260822.05__index_customer_management_search.sql` | Customer | Search indexes on `customer_management_customer` | No FK | Customer | `DELETE_HISTORY` | V200 |

### Payment — target V300

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V2026080101__create_payment_persistence.sql` | Payment | `payments` | No external FK | Payment | `MERGE_IN_BASELINE` | V300 |
| `V2026080102__create_payment_audit.sql` | Payment | `payment_audit`, append-only function/triggers | FK → `payments` | Payment | `MERGE_IN_BASELINE` | V300 |
| `V2026080103__create_payment_outbox.sql` | Payment | `payment_outbox_events` | FK aggregate_id → `payments` | Payment | `MERGE_IN_BASELINE` | V300 |
| `V2026080104__create_payment_idempotency.sql` | Payment | `payment_idempotency` | Optional FK → `payments` | Payment | `MERGE_IN_BASELINE` | V300 |
| `V2026080105__payment_state_schema_v2.sql` | Payment | Alters `payments.ck_payments_state_schema_version` from schema 1 to schemas 1/2 | No FK | Payment | `DELETE_HISTORY` | V300 |

### Accounting — target V400

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V202608071100__accounting_batches.sql` | **Bootstrap** | `accounting_batches`, `accounting_batch_items` | Item → Batch only. `payment_id` and `partner_id` are logical references; no Payment/Partner FK | Accounting | `MOVE_AND_MERGE` | V400 |
| `V202608071200__accounting_batch_tracking.sql` | **Bootstrap** | `accounting_batch_tracking`, `accounting_batch_item_tracking` | Tracking → Accounting batch/item tables only | Accounting | `MOVE_AND_MERGE` | V400 |

### Reporting — target V500

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V202608072058__create_reporting_payment_audit_projection.sql` | **Bootstrap** | `reporting_payment_audit_evidence` | `payment_id` and `observed_customer_id` are projection facts; no SQL FK | Reporting | `MOVE_AND_MERGE` | V500 |
| `V202608072120__create_reporting_audit_export.sql` | **Bootstrap** | Alters audit evidence with `financial_institution_code`; creates `reporting_payment_audit_export_job` | No FK | Reporting | `MOVE_AND_MERGE` | V500 |

### Notification — target V600

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V202607272300__create_notification_deliveries.sql` | Notification | `notification_deliveries` | `event_id`, `aggregate_id` are logical references; no external FK | Notification | `MERGE_IN_BASELINE` | V600 |
| `V202607280100__add_notification_delivery_retry_payload.sql` | Notification | Adds `notification_deliveries.reason` | No FK | Notification | `DELETE_HISTORY` | V600 |
| `V202608071300__operational_notifications.sql` | Notification | `sixpay.operational_notification_deliveries`, `sixpay.operational_notification_attempts` | Attempts → operational delivery only | Notification | `MERGE_IN_BASELINE` | V600 |
| `V202608071400__operational_notification_operations.sql` | Notification | Alters operational delivery retry/replay fields; creates `sixpay.operational_notification_replays` | Replay → operational delivery only | Notification | `MERGE_IN_BASELINE` | V600 |
| `bootstrap/V202608071300__operational_notifications.sql` | **Bootstrap** | Exact physical duplicate of Notification migration | Same Notification-local FKs | Notification | `DELETE_DUPLICATE` | V600 |
| `bootstrap/V202608071400__operational_notification_operations.sql` | **Bootstrap** | Exact physical duplicate of Notification migration | Same Notification-local FKs | Notification | `DELETE_DUPLICATE` | V600 |

### Security — target V700

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V202608102130__security_local_authentication.sql` | **Bootstrap** | `security_local_users`, temporary `security_local_user_authorities`, `security_authentication_audit`, append-only audit trigger/function | Authorities → local users | Security | `MOVE_AND_MERGE` | V700 |
| `V202608110200__security_identity_linking.sql` | **Bootstrap** | Creates `security_user_accounts`, `security_user_identities`; migrates Local data; adds `security_local_users.user_id` | Identity/local credential → canonical user account | Security | `MOVE_AND_MERGE` | V700 |
| `V202608110300__security_user_authorization.sql` | **Bootstrap** | Creates `security_user_roles`, `security_user_permissions`; migrates authorities; drops temporary `security_local_user_authorities` | Roles/permissions → canonical user account | Security | `MOVE_AND_MERGE` | V700 |
| `V202608110400__security_operational_audit.sql` | **Bootstrap** | `security_audit_events`, append-only trigger/function | Optional target_user_id → canonical user account | Security | `MOVE_AND_MERGE` | V700 |
| `V202608110500__security_user_crud_audit.sql` | **Bootstrap** | Widens `security_audit_events.event_type` constraint | No new FK | Security | `DELETE_HISTORY` | V700 |
| `V202608152000__security_local_credential_lifecycle.sql` | **Bootstrap** | Adds password lifecycle columns/constraints/index to `security_local_users` and migrates existing values | No new FK | Security | `DELETE_HISTORY` | V700 |
| `V202608152100__security_password_history.sql` | **Bootstrap** | `security_password_history` | FK → canonical user account | Security | `MOVE_AND_MERGE` | V700 |
| `V202608160001__security_audit_event_types_da10.sql` | **Bootstrap** | Final widening of `security_audit_events.event_type` including `PASSWORD_CHANGED` | No new FK | Security | `DELETE_HISTORY` | V700 |

### Administration — target V800

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V202608231130__create_operational_incident_tables.sql` | Administration | `operational_incident`, `operational_incident_timeline` | Timeline → incident only. `accounting_batch_id`, `payment_id`, `payment_reference` are logical references with no cross-domain FK | Administration | `KEEP_AS_BASELINE_CONTENT` | V800 |

### Cross-domain candidate — explicit review required

| Migration | Current owner | Tables / objects | References / FKs | Target owner | Decision | Target baseline |
|---|---|---|---|---|---|---|
| `V202608071900__payment_observed_customer_query_link.sql` | **Bootstrap** | `payment_observed_customer_link` | **SQL FK payment_id → Payment.payments**; `observed_customer_id` has deliberately no FK to Customer | **Payment** | `MERGE_IN_BASELINE` | V300 |

Current evidence favors Payment ownership because:

- `payment_id` is the primary key;
- the only SQL FK is to `payments`;
- deletion cascades from Payment;
- `observed_customer_id` is a logical UUID only.

FS-2.3.3 confirms Payment ownership from the table lifecycle and FK structure.

## Cross-domain SQL dependency findings

Healthy logical references with no cross-domain FK currently include:

- Customer Subscription → `partner_id`;
- Customer Observed Payment/Audit → Payment identifiers;
- Accounting Batch Item → Payment / Partner identifiers;
- Reporting Evidence → Payment / Observed Customer identifiers;
- Notification → source event / aggregate identifiers;
- Administration Incident → Accounting / Payment identifiers.

The only current migration requiring a bounded-context ownership decision is
`payment_observed_customer_link`.

## Baseline target after FS-2.3

```text
backend/
├── partner/src/main/resources/db/migration/
│   └── V100__partner_baseline.sql
├── customer/src/main/resources/db/migration/
│   └── V200__customer_baseline.sql
├── payment/src/main/resources/db/migration/
│   └── V300__payment_baseline.sql
├── accounting/src/main/resources/db/migration/
│   └── V400__accounting_baseline.sql
├── reporting/src/main/resources/db/migration/
│   └── V500__reporting_baseline.sql
├── notification/src/main/resources/db/migration/
│   └── V600__notification_baseline.sql
├── security/src/main/resources/db/migration/
│   └── V700__security_baseline.sql
├── administration/src/main/resources/db/migration/
│   └── V800__administration_baseline.sql
└── bootstrap/src/main/resources/
    └── # no business db/migration
```

## FS-2.3.0 exit criteria

FS-2.3.0 is complete when:

- all current migrations are inventoried;
- every migration has a current owner;
- every table/object touched is identified;
- every SQL FK relevant to ownership is identified;
- every migration has a target owner;
- every migration has one decision from the controlled vocabulary;
- every migration maps to a target baseline;
- `payment_observed_customer_link` is explicitly classified as Payment-owned;
- no SQL migration has yet been changed or deleted.

## Decision

FS-2.3.0 freezes the repository migration state as **35 physical migration
files** before consolidation.

The inventory confirms that Bootstrap currently owns no unique platform schema.
Every Bootstrap migration is either:

- business-domain content to evacuate;
- an exact duplicate to delete; or
- the single cross-domain ownership candidate requiring explicit review.

Target criterion:

```text
business-owned migration in bootstrap = 0
```
