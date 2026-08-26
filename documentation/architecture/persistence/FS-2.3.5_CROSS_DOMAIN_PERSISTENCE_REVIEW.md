# FS-2.3.5 — Cross-Domain Persistence Review

**Branch:** `feat/repository-baseline-consolidation-cleanup`
**Gate:** `FS-2.3 — Database baseline consolidation`
**Status:** Reviewed
**Golden module:** Partner

## Purpose

FS-2.3.5 verifies that a domain-owned database baseline does not create a
physical SQL foreign key to a table owned by another bounded context.

Canonical rule:

```text
table A owner X
    REFERENCES table B owner Y

if X != Y
    => forbidden by default
```

Cross-domain relationships remain logical references unless an explicit
architecture decision authorizes a physical dependency.

## Reviewed baselines

| Baseline | Owner | Physical FK target ownership | Result |
|---|---|---|---|
| `V100__partner_baseline.sql` | Partner | Partner only | PASS |
| `V200__customer_baseline.sql` | Customer | Customer only | PASS |
| `V300__payment_baseline.sql` | Payment | Payment only | PASS |
| `V400__accounting_baseline.sql` | Accounting | Accounting only | PASS |
| `V500__reporting_baseline.sql` | Reporting | No external FK | PASS |
| `V600__notification_baseline.sql` | Notification | Notification only | PASS |
| `V700__security_baseline.sql` | Security | Security only | PASS |
| `V800__administration_baseline.sql` | Administration | Administration only | PASS |

## Review conclusion

Current canonical baseline:

```text
cross-domain physical FK = 0
```

All discovered physical FK targets are owned by the same module baseline as the
referencing persistence model.

No exception or ADR is required for the current baseline.

### Important logical cross-domain references

These identifiers remain logical only and intentionally have no SQL FK to another domain:

- Customer: `partner_id`, `payment_id`
- Payment: `observed_customer_id`
- Accounting: `payment_id`, `partner_id`
- Reporting: `payment_id`, `observed_customer_id`
- Notification: `event_id`, `aggregate_id`
- Administration: `accounting_batch_id`, `payment_id`, `payment_reference`

### `payment_observed_customer_link`

This table is Payment-owned.

```text
payment_observed_customer_link.payment_id
    -> payments.payment_id
```

Both source and target are Payment-owned.

`observed_customer_id` remains a logical Customer reference and has no FK to a
Customer table.

## Non-regression rule

Every table named by a `REFERENCES` clause in a canonical Vx00 baseline must
also be created by that same Vx00 baseline.

This stricter rule is valid for the current one-baseline-per-domain model and
automatically rejects a direct cross-domain FK.

## Exit criteria

FS-2.3.5 is complete when:

- all V100–V800 baselines have been inspected;
- every physical FK resolves to the same domain baseline;
- `payment_observed_customer_link` is formally Payment-owned;
- logical cross-domain identifiers remain unconstrained by external SQL FKs;
- current cross-domain physical FK count is zero;
- a non-regression architecture test enforces the rule.

## Decision

```text
same-domain FK       = ALLOWED
cross-domain FK      = FORBIDDEN BY DEFAULT
logical cross-domain = ALLOWED

current cross-domain FK count = 0
```
