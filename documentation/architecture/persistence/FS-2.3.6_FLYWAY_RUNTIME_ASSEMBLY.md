# FS-2.3.6 — Flyway Runtime Assembly

**Branch:** `feat/repository-baseline-consolidation`  
**Gate:** `FS-2.3 — Database baseline consolidation`  
**Status:** Runtime assembly  
**Golden module:** Partner

## Canonical distinction

```text
migration execution owner  = Bootstrap runtime
migration definition owner = business module
```

Bootstrap assembles and executes Flyway. Business modules own the SQL.

## Runtime model

```text
bootstrap application
        ↓
bootstrap runtime classpath
        ↓
V100 Partner
V200 Customer
V300 Payment
V400 Accounting
V500 Reporting
V600 Notification
V700 Security
V800 Administration
        ↓
Flyway classpath:db/migration
        ↓
schema sixpay
```

## Required invariants

- Bootstrap depends on all eight migration-owning modules.
- Bootstrap owns `spring-boot-starter-flyway`, PostgreSQL Flyway support and PostgreSQL runtime driver.
- `spring.flyway.locations = classpath:db/migration`.
- `spring.flyway.schemas = sixpay`.
- `spring.flyway.default-schema = sixpay`.
- Bootstrap contains no business migration SQL.
- V100–V800 are discoverable from the assembled runtime classpath.
- Historical `V2026...` migrations must not be executed.

Future V101/V301/etc are allowed, so the runtime gate verifies required canonical baselines rather than permanently asserting an exact migration count.

## Runtime proof

`FlywayModuleAssemblyIT` starts an empty PostgreSQL Testcontainer and runs Flyway against `classpath:db/migration`.

It verifies:

1. V100, V200, V300, V400, V500, V600, V700 and V800 are applied.
2. No historical version beginning with `2026` is applied.
3. `flyway_schema_history` is created in schema `sixpay`.
4. A representative root table exists for every persistence owner.

Representative objects:

- Partner → `partners`
- Customer → `customer_management_customer`
- Payment → `payments`
- Accounting → `accounting_batches`
- Reporting → `reporting_payment_audit_evidence`
- Notification → `operational_notification_deliveries`
- Security → `security_user_accounts`
- Administration → `operational_incident`

## Decision

```text
Bootstrap = runtime assembler + Flyway executor
Business module = database object owner + migration SQL owner
```
