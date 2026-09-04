# FS-2.3.8 — Database Baseline Governance Gate

**Revision policy:** Task-selected authoritative revision
**Gate:** `FS-2.3 — Database baseline consolidation`
**Status:** Non-regression gate
**Golden module:** Partner

## Purpose

FS-2.3.8 converts the FS-2.3 persistence decisions into permanent architecture
rules.

The gate must fail on:

```text
business *.sql in bootstrap resources
migration outside owner range
duplicate Flyway version
unapproved cross-domain FK
persistence entity without migration ownership
historical pre-baseline migration restored
```

## Canonical ranges

| Owner | Range |
|---|---:|
| Partner | 100–199 |
| Customer | 200–299 |
| Payment | 300–399 |
| Accounting | 400–499 |
| Reporting | 500–599 |
| Notification | 600–699 |
| Security | 700–799 |
| Administration | 800–899 |
| Platform | 900–999 |

Platform is exceptional and has no migration by default.

## Gate composition

FS-2.3.8 is intentionally composed from two complementary architecture tests.

### DatabaseBaselineGovernanceArchitectureTest

Checks:

1. Bootstrap contains no Flyway-shaped SQL.
2. Every migration lives in the range reserved to its owning module.
3. Flyway versions are globally unique across module resources.
4. No historical `V2026...` SQL exists anywhere under runtime resources.
5. Every backend module containing a production `@Entity` has explicit
   migration ownership and at least one canonical Flyway migration.
6. A module with persistence entities but no reserved range fails immediately.

### CrossDomainForeignKeyArchitectureTest

Existing FS-2.3.5 gate checks:

```text
REFERENCES target
```

and requires the target table to be created by the same canonical domain
baseline.

Current explicit cross-domain FK allow-list:

```text
EMPTY
```

Therefore every physical cross-domain FK is rejected.

If a future exception is ever accepted, it must be introduced through an
explicit architecture decision and an explicit allow-list change; it must never
become legal merely because two modules share the same PostgreSQL database.

## Historical migration policy

The pre-baseline migration generation is identified by the former timestamp
versions:

```text
V2026...
```

No such SQL file may be restored under any production runtime resource path,
including non-standard paths such as:

```text
db/security/migration
```

Canonical runtime migration ownership is exclusively the V100–V899 domain
ranges plus explicitly justified V900–V999 platform migrations.

## Persistence ownership rule

The gate scans each backend module's:

```text
src/main/java
```

for production JPA entities.

If `@Entity` is present, that module must:

- be present in the controlled ownership/range map;
- own `src/main/resources/db/migration`;
- contain at least one Flyway migration in its own range.

This prevents a future module from adding JPA persistence while silently
placing its DDL in Bootstrap or another module.

## Duplicate version rule

Flyway versions are global on the assembled Bootstrap classpath.

Therefore:

```text
Partner/V101
Payment/V101
```

is illegal even though the files live in different JARs.

The gate parses every canonical module migration and rejects any duplicate
numeric Flyway version.

## Exit criteria

FS-2.3.8 is DONE when:

- the consolidated governance test is installed;
- the existing cross-domain FK gate remains green;
- Bootstrap migration ownership gate remains green;
- `mvn -pl bootstrap -am test` is green;
- the full reactor `mvn verify` remains green.

## Decision

```text
FS-2.3 persistence policy
        ↓
architecture tests
        ↓
automatic regression prevention
```
