# SIXPAY CONNECT — Registre des contrats

Ce dossier contient les contrats d’intégration et d’API versionnés de
SIXPAY CONNECT.

La présence physique d’un contrat dans le dépôt ne détermine pas à elle seule
son usage courant. La classification normative et l’index cross-domain sont
portés par [`CONTRACT_REGISTRY.yaml`](./CONTRACT_REGISTRY.yaml).

## Canonical contract index

`CONTRACT_REGISTRY.yaml` is the **canonical contractual table of contents** for
the current SIXPAY CONNECT repository baseline.

It answers the repository-level governance questions:

- which contracts exist;
- which domain and capability they belong to;
- who owns the capability and security boundary;
- the interaction direction;
- source system and system of record;
- lifecycle and approval state;
- generation policy;
- security classification;
- current MVP usage;
- the canonical physical contract path.

The registry is therefore authoritative for **contract classification,
ownership, lifecycle, approval, generation policy and usage metadata**.

Physical contract files remain authoritative for the **interface itself**:

- paths/endpoints;
- operations;
- request and response payloads;
- schemas;
- parameters;
- protocol-level security declarations;
- error responses;
- event/message structure for asynchronous contracts.

The intended relationship is:

```text
CONTRACT_REGISTRY.yaml
        |
        +-- contract identity / capability
        +-- ownership / direction
        +-- lifecycle / approval
        +-- generation policy
        +-- security classification
        +-- MVP usage
        +-- canonical physical path
                    |
                    v
          physical contract file
                    |
                    +-- interface/protocol truth
```

A physical contract must not become a second independent registry.
Conversely, the registry must not duplicate full interface definitions.

When a physical contract repeats governance metadata through
`info.x-sixpay-contract`, that metadata is a contract-local mirror used for
traceability and validation. It must remain consistent with
`CONTRACT_REGISTRY.yaml`; the registry remains the canonical cross-contract
index.

### Source-of-truth rule

For the current repository baseline:

1. `CONTRACT_REGISTRY.yaml` is authoritative for registry-level governance
   metadata.
2. The physical contract referenced by `path` is authoritative for the
   interface/protocol definition.
3. Git history is authoritative for historical changes.
4. Transitional patch artifacts are not valid current-state sources of truth.

This separation allows the future Master AI Context to discover the complete
contract landscape from a single registry without flattening bounded API and
integration contracts into one monolithic specification.

## Registry semantic vocabulary

`classificationModel` defines the controlled vocabulary used by registry
entries.

The registry normalizes domains, ownership values, known systems, direction
endpoints, data classifications, pagination modes, lifecycle statuses,
approval statuses and generation policies.

Capabilities use `UPPER_SNAKE_CASE`.

Direction supports either `direction: SOURCE_TO_TARGET` or the explicit
`primaryDirection` + `fallbackDirection` pair used by a true bidirectional
operational contract. A registry entry must not mix both shapes.

`mvpUsage.included` is mandatory and must agree with lifecycle semantics:
`ACTIVE_MVP` and `REFERENCE_MVP` are included; `DEFERRED_FUTURE` is excluded.

Security mechanisms remain contract-specific because external APIs, internal
JWT APIs, in-process semantic contracts and SMTP delivery do not share one
authentication model. When `security.dataClassification` is present, its value
must come from the controlled vocabulary.

## Consolidated-contract cardinality

Registry identity and physical-file identity are intentionally different
concepts.

The default model is:

```text
1 registry capability
        ↓
1 physical contract
```

A bounded API ownership may intentionally consolidate several logical
capabilities into one physical contract:

```text
N registry capabilities
        ↓
1 physical contract
```

For such a consolidation:

- every registry `id` remains globally unique;
- every registry `capability` remains globally unique;
- `path` is allowed to be non-unique;
- entries sharing a path must remain within one coherent `domain`;
- entries sharing a path must have the same `businessOwner`;
- the physical OpenAPI contract must declare
  `info.x-sixpay-contract.registryIds`;
- it must also declare `info.x-sixpay-contract.capabilities`;
- those two arrays must exactly match the registry entries sharing the path.

The current canonical example is:

```text
administration-query-api-v1
  capability: ADMINISTRATION_OPERATIONAL_QUERY ─┐
                                                │
                                                ├─>
                                                │  administration-operational-api-v1.yaml
                                                │
incident-query-api-v1                           │
  capability: OPERATIONAL_INCIDENT_QUERY ───────┘
```

The two capabilities remain independently classifiable in the registry while
sharing one coherent Administration-owned OpenAPI boundary.

Physical consolidation must never be used to hide ownership differences or to
collapse unrelated capabilities merely to reduce file count.

## Registry ↔ filesystem integrity

The canonical registry and the physical contract tree must remain consistent in
both directions.

The following invariants are mandatory:

1. every `contracts[*].path` declared by `CONTRACT_REGISTRY.yaml` must resolve
   to an existing file;
2. a registry path must never reference a historical/transitional artifact;
3. every canonical physical contract must be referenced by at least one
   registry entry;
4. multiple capabilities may reference the same physical contract when the
   consolidation is intentional and ownership remains explicit.

The Administration Operational contract is the current canonical example of
rule 4:

```text
ADMINISTRATION_OPERATIONAL_QUERY ─┐
                                  ├─> administration-operational-api-v1.yaml
OPERATIONAL_INCIDENT_QUERY ───────┘
```

The integrity gate treats the following roots as the current canonical
physical-contract baseline:

- `documentation/contracts/amplitude/`;
- `documentation/contracts/tresorpay/`;
- `documentation/contracts/internal/`.

Within those roots, YAML/YML/JSON specifications are physical contracts.
Markdown contracts are canonical only when directly under
`documentation/contracts/internal/`.

Other trees such as `external/`, `integration/` and `events/` remain repository
inventory until an explicit consolidation decision promotes, supersedes or
removes them. Their physical presence alone does not make them canonical.

Governance documents such as this `README.md` are not physical contracts and
must not be registered as capabilities.

## Historical artifact policy

`documentation/contracts/` describes the **current contractual baseline**.
It must not contain files whose purpose is to preserve an intermediate change,
patch or local correction.

The following artifact families are forbidden from the canonical contract tree:

- `*.patch`;
- `*.diff`;
- `*.rej`;
- `*.orig`;
- `*.bak`;
- `*.tmp`;
- Markdown patch documents such as `*_PATCH.md`, `*-PATCH.md`,
  `PATCH_*.md` or equivalent patch-named files.

Historical contract evolution belongs to **Git history**.

A contract change must therefore finish by updating the canonical physical
contract and, when applicable, `CONTRACT_REGISTRY.yaml`. Temporary change
artifacts must not survive in the repository baseline.

The historical-artifact gate is intentionally pattern-based rather than tied
to specific retired filenames. This keeps the baseline independent from
development-history artifacts while preventing equivalent patch, backup or
temporary files from being reintroduced.

## Contract lifecycle and current usage

Current lifecycle, approval, MVP inclusion and generation permissions are defined
by `CONTRACT_REGISTRY.yaml` and the governance metadata mirrored in each
canonical physical contract.

A retained or deferred contract is not automatically active and must not drive
MVP implementation or code generation unless its registry status allows it.

Internal read-only contracts remain separate where ownership, security, data
classification or operational semantics differ. Payment Query remains distinct
from privileged Payment Audit/export; ObservedCustomer remains distinct from
authoritative Customer Management; Accounting Batch Query remains
Accounting-owned; Notification inbound trigger and outbound delivery boundaries
remain separate.

Any contract-classification change must update the registry and affected
canonical contract metadata in the same change.

## Accounting T1 manual execution

`internal/accounting-t1-manual-execution-api-v1.yaml` is the approved internal
operator command for launching SIXPAY T1 manually. It remains distinct from the
read-only Accounting Query API, the approved Amplitude Accounting Entries
provider contract and the bank-authoritative End-of-Day Confirmation contract.

The provisional `external/accounting/accounting-batch-*` pack is superseded by
the approved Amplitude Accounting Entries contract and is removed.

### Accounting TFJ Operational Query

`accounting-tfj-operational-query-api-v1` defines the internal read-only
operator surface for TFJ and reconciliation visibility.

The contract provides search and detail only. It exposes normalized
Accounting-owned operational facts and is intentionally separate from the
provider-facing `amplitude-end-of-day-confirmation-api-v1`.

No retry, replay, force-match, resolve, reverse or mark-integrated command is
defined by this contract.
