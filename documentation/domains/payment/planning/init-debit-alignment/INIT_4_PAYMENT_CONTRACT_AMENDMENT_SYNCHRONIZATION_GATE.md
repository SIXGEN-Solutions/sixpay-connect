# SIXPAY CONNECT — INIT-4
## Payment Contract Amendment & Synchronization Gate

### Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- Baseline SHA: `8e5f6eb65ed412e589f970ab48a886dd219533d9`
- Mode: local contract/documentation amendment preparation
- Public Java DTO modification: **NO**
- Remote Git mutation: **NO**

## Purpose

INIT-4 is not an approval gate for a missing or previously unapproved contract.

`tresorpay-payment-request-api-v1` already exists and is classified:

```text
lifecycleStatus: ACTIVE_MVP
approvalStatus: APPROVED
generationPolicy: ACTIVE
codeGenerationAllowed: true
```

INIT-4 prepares the precise amendment induced by INIT-0..INIT-3, obtains human
validation of that amendment, then synchronizes the physical contract, registry
semantics when needed, and canonical planning documentation.

This gate remains mandatory before changing the public Payment request DTO when
the wire contract changes.

## Contract changes prepared

The preserved InitiateDebit payload is:

| Field | Cardinality | Semantic |
|---|---|---|
| `LoginName` | REQUIRED | compatibility metadata, not authentication |
| `AppID` | REQUIRED | canonical Partner `partnerIdentifier` |
| `endToEndId` | REQUIRED | Partner-scoped external payment reference |
| `montantTotal` | REQUIRED | requested amount |
| `devise` | OPTIONAL | defaults to `XAF` when omitted for the current MVP; supplied ISO currency is propagated for authoritative Core Banking validation |
| `ribDebiteur` | OPTIONAL | account hint/control after NIU-first resolution |
| `nomDebiteur` | OPTIONAL | non-authoritative descriptive input |
| `typeCreance` | REQUIRED | claim type |
| `NUI` | REQUIRED | primary banking customer-resolution pivot |
| `dateExecution` | REQUIRED | requested execution instant |
| `beneficiaires` | OPTIONAL | no automatic allocation invented |
| `callbackURL` | REQUIRED | HTTPS callback |

No JSON `partnerIdentifier`, `RIP` or `accountNumber` is introduced.

## Partner identity rule

```text
authenticated machine subject
    -> Security machine identity link
    -> registered Partner
    -> Partner.partnerIdentifier
    == request.AppID
```

`X-TresorPay-App-Id` remains independent transport/integration metadata.
It may equal `AppID`, but equality is not required.

## NIU / account rule

```text
NUI required
  -> authoritative Core Banking customer resolution
  -> debtor account may be unknown at intake
  -> optional ribDebiteur may be used as a selector/control
  -> authoritative bank result wins
```

## Registry decision

No lifecycle, approval or generation-state downgrade is introduced by this lot.

The registry remains:

```text
ACTIVE_MVP / APPROVED / ACTIVE / codeGenerationAllowed=true
```

Only descriptive purpose/constraints are synchronized with the approved
INIT-3 semantics.

## Human gate

Before INIT-5 changes the public DTO/wire implementation, a human reviewer must
validate the physical OpenAPI amendment, in particular:

1. preserved request-field names and cardinalities;
2. `AppID` business semantics;
3. independence of `X-TresorPay-App-Id`;
4. NIU-first / optional-RIB semantics;
5. optional `devise` defaulting to `XAF` when omitted for the current MVP, while `beneficiaires` remains without automatic allocation;
6. compatibility implications for existing consumers.

## Explicitly not solved by INIT-4

INIT-4 does not invent:

- automatic Treasury beneficiaries;
- new public error codes;
- a new authentication mechanism;
- a new Core Banking endpoint;
- idempotency/schema changes belonging to the following lot.

## Exit criteria

- physical Payment Request contract reflects the approved target wire shape;
- registry contract status remains synchronized;
- INIT-3 is marked technically closed;
- planning index is updated;
- no public Java DTO is changed before human review;
- contract/documentation gates pass.

Status:

```text
INIT-4 — CONTRACT AMENDMENT PREPARED; HUMAN VALIDATION REQUIRED BEFORE DTO CHANGE
```

## INIT-7 currency clarification

The human-approved MVP rule is:

```text
devise omitted
  -> SIXPAY defaults to XAF
  -> Core Banking performs authoritative currency validation

devise supplied
  -> SIXPAY normalizes and propagates the ISO alpha-3 value
  -> Core Banking performs authoritative currency validation
```

`devise` remains optional on the public wire contract. `XAF` is the current MVP
default because the first deployment context is a Central African financial
environment. Making the default currency environment-configurable is explicitly
deferred to a future evolution.

SIXPAY must not reject a syntactically valid supplied currency merely because it
is different from XAF. Final banking acceptance belongs to Core Banking.
