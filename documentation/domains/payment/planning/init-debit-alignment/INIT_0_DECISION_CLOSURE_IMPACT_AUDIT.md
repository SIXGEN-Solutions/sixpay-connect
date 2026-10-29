# SIXPAY CONNECT — INIT-0
## Decision Closure & Impact Audit

### Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- SHA: `2c7a575674fecaae044a23ac0663de7ea9763387`
- Mode: documentary decision closure and impact audit only
- No Java change
- No physical OpenAPI change
- No migration
- No code generation
- No commit/push
- No gate executed

---

# 1. Inputs

INIT-0 consumes:

- the approved project governance sources;
- current implementation at the reference SHA;
- `SIXPAY_INIT_1_PARTNER_CUSTOMER_IDENTIFICATION_CONTRACT_PROPOSAL.md`;
- current Payment and Customer Verification contracts.

The INIT-1 decision input establishes:

```text
AppID == partnerIdentifier
NUI == required NIU pivot
ribDebiteur == optional
nomDebiteur == optional
devise == optional
beneficiaires == optional
```

The physical payload names remain unchanged.

---

# 2. Decisions closed by INIT-0

## 2.1 Canonical partner identifier

Closed decision:

```text
payload AppID == canonical business partnerIdentifier
```

Consequences:

- no new JSON property `partnerIdentifier`;
- `AppID` is target REQUIRED;
- `LoginName` remains in the payload but is not the durable business identity;
- Payment must not use an authentication login as the durable Partner business key.

Observed current gap:

- `InitiateDebitCommand` still carries:
  - `partnerLoginName`;
  - `authenticatedPartnerLoginName`;
  - `applicationId`;
- it enforces exact equality between declared and authenticated login names;
- `applicationId` is still optional.

Therefore the current application identity model is not yet aligned with the closed decision.

---

## 2.2 Relation with Security identity

Closed semantic rule:

```text
Security-authenticated SIXPAY identity
        ↓
resolve authorized Partner
        ↓
canonical partnerIdentifier
        ↓
MUST MATCH
        ↓
AppID declared in InitiateDebit
```

`AppID` alone is never authentication.

`LoginName` may remain a compatibility/declarative request field, but Partner
authorization must be based on the trusted authenticated identity and canonical
Partner resolution.

Observed current gap:

- `PaymentPartnerIsolationPolicy` currently scopes Partner access through
  `AuthenticatedUser.subject()`;
- `InitiateDebitCommand` validates login-to-login equality;
- no current evidence in INIT-0 proves that `AuthenticatedUser.subject()` is
  the Partner business identifier.

INIT-0 therefore closes the semantic relationship but does not invent the
concrete Partner public surface. That implementation design remains for INIT-2.

---

## 2.3 Technical idempotency scope

Closed target namespace:

```text
(partnerIdentifier, operation, idempotencyKey)
```

Observed current persistence:

```text
UNIQUE(operation, idempotency_key)
```

Observed current lookup:

```text
findByOperationAndIdempotencyKey(...)
```

Therefore current technical idempotency is global across Partners and is not
multi-partner safe if two Partners can legitimately reuse the same
`Idempotency-Key`.

No schema or migration is changed in INIT-0.

---

## 2.4 Canonical request fingerprint

Closed rule for future alignment:

- canonical Partner business identity must participate in the initiation
  fingerprint;
- `AppID` is that business identity;
- trusted transport-only identity and transport correlation metadata are not
  interchangeable with the business key;
- optional request fields must be canonicalized deterministically;
- `Idempotency-Key` and correlation ID remain excluded from the business
  fingerprint.

Observed current canonicalizer includes:

```text
partnerLoginName
authenticatedPartnerLoginName
applicationId
endToEndId
amount
currency
debtorRib
debtorName
claimType
taxpayerIdentifier
requestedExecutionAt
callbackUrl
beneficiaries
```

This must be revisited once canonical Partner resolution is implemented.

---

## 2.5 External payment reference scope

Closed business identity:

```text
(partnerIdentifier, externalPaymentReference)
```

Mapping in preserved payload:

```text
partnerIdentifier         -> AppID
externalPaymentReference  -> endToEndId
```

Reason:

different Partners may legitimately use the same external reference value.

INIT-0 does not change persistence or unique constraints.

The next persistence/idempotency lot must audit every lookup and uniqueness
constraint that currently treats `externalPaymentReference` as globally unique.

---

# 3. NIU and account semantics closed

## 3.1 NIU remains REQUIRED

Closed rule:

- physical field remains `NUI`;
- Java semantic remains `taxpayerIdentifier`;
- NIU is the mandatory pivot for banking customer resolution.

## 3.2 RIB becomes OPTIONAL

Closed rule:

```text
NIU first
  ↓
resolve customer
  ↓
retrieve/resolve eligible account(s)
  ↓
if ribDebiteur was supplied:
    use it only to select/control among authoritative accounts
```

A supplied RIB does not override banking facts.

## 3.3 Supplied RIB not found

Closed semantic outcome:

if the supplied RIB does not correspond to any account belonging to the
NIU-resolved customer, the request is a functional account-resolution failure.

The exact public HTTP/status/error code is not invented by INIT-0 and remains a
contract decision.

## 3.4 Multiple eligible accounts without RIB

Closed processing rule:

SIXPAY must not choose an account arbitrarily.

If the bank response does not provide one authoritative selected account and
multiple eligible accounts remain, the resolution is ambiguous.

Target semantic error category:

```text
CUSTOMER_ACCOUNT_RESOLUTION_AMBIGUOUS
```

This is a documentary semantic category, not yet an approved public API code.

---

# 4. Exact inventory — current dependencies on a known debtor account/RIB

The current implementation is account-first in multiple places.

## API boundary

### `backend/payment/.../api/request/InitiateDebitRequest.java`

Current:

```text
ribDebiteur -> @NotBlank
```

Impact:
REST rejects a request without RIB before NIU-first resolution can begin.

---

## Application command

### `backend/payment/.../application/command/InitiateDebitCommand.java`

Current:

```text
debtorRib = requireText(...)
```

Impact:
application layer still requires a known RIB.

---

## API mapper

### `backend/payment/.../api/PaymentCommandApiMapper.java`

Current:

```text
request.debtorRib() -> InitiateDebitCommand
```

Impact:
mapper assumes the field is part of the immediate command context.

It also dereferences:

```text
request.beneficiaries().stream()
```

so the separate decision `beneficiaires OPTIONAL` will require null-safe
alignment later.

---

## Payment initiation domain

### `backend/payment/.../domain/model/NewPaymentIntent.java`

Observed invariant:

```text
debtorAccountReference != null
```

and the account's financial institution must equal the Payment institution.

Impact:
Payment cannot currently be created without a resolved debtor account
reference.

This is a major structural dependency: merely making `ribDebiteur` nullable at
REST/command level is insufficient.

---

## Payment aggregate

### `backend/payment/.../domain/model/Payment.java`

At `receive(...)`, Payment state persists:

```text
debtorAccountReference
```

and `PaymentReceived` uses:

```text
debtorAccountReference.maskedDisplay()
```

At banking verification start, the event uses:

```text
debtorAccountReference.bindingFingerprint()
```

Impact:
the aggregate currently assumes account evidence exists before Customer
Verification.

This conflicts with a pure NIU-first flow where the account is discovered by
Customer Verification.

---

## Payment Customer Verification request

### `backend/payment/.../application/port/output/CustomerVerificationRequest.java`

Current required fields include:

```text
accountBindingFingerprint
integrationAccountToken
```

Both are non-null required.

Impact:
Payment's Customer Verification port cannot currently express NIU-only
resolution.

---

## Customer Verification request factory

### `backend/payment/.../application/service/PaymentCustomerVerificationRequestFactory.java`

Current factory derives:

```text
state.debtorAccountReference().bindingFingerprint()
state.debtorAccountReference().integrationAccountToken()
```

Impact:
Customer Verification cannot even be requested without an already-known
`DebtorAccountReference`.

---

## Customer domain verification request

### `backend/customer/.../verification/domain/model/CustomerVerificationRequest.java`

Current invariant requires:

```text
AccountBindingFingerprint
```

Impact:
Customer domain itself models verification as customer + pre-bound account,
not customer-first account resolution.

---

## Customer banking query

### `backend/customer/.../verification/application/port/output/BankingVerificationQuery.java`

Current required fields include:

```text
accountBindingFingerprint
bankingAccountAccessReference
```

Impact:
banking verification is account-addressed before the bank call.

---

## Amplitude mapper

### `backend/customer/.../verification/infrastructure/banking/mapper/AmplitudeCustomerVerificationMapper.java`

Current external request constructs an account subject from:

```text
query.bankingAccountAccessReference().value()
```

Impact:
the adapter calls the composite verification contract with an already-known
account reference.

---

## Physical Core Banking verification contract

### `documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`

Observed:

```text
CustomerVerificationRequest.account REQUIRED
AccountVerificationSubject requires one of:
- accountReference
- rib
- iban
```

Impact:
the existing composite verification operation is account-required.

The same contract also exposes customer/account search capabilities that must be
reviewed in INIT-3 before inventing any new endpoint.

---

## Persistence

### `backend/payment/.../infrastructure/persistence/PaymentStateDocument.java`

Current persisted Payment state includes non-null domain
`DebtorAccountReference`.

Impact:
a future NIU-first implementation may require a Payment-state evolution if
Payment is persisted before banking account resolution.

No persistence schema change is authorized by INIT-0.

---

# 5. Partner inconsistency error semantics

Closed semantic condition:

```text
resolved authenticated partnerIdentifier != request.AppID
```

Target semantic error category:

```text
PARTNER_IDENTITY_MISMATCH
```

Classification:
security/authorization rejection.

INIT-0 deliberately does not assign an HTTP status or public ProblemDetail code.
That requires contract approval.

Partner not resolvable from the authenticated principal remains a distinct
condition from declared/authenticated mismatch.

---

# 6. Customer ambiguity error semantics

Closed semantic condition:

```text
NIU resolves a customer
AND no ribDebiteur is supplied
AND more than one eligible account remains
AND Core Banking does not authoritatively select one
```

Target semantic error category:

```text
CUSTOMER_ACCOUNT_RESOLUTION_AMBIGUOUS
```

No HTTP status/public code is invented in INIT-0.

---

# 7. OpenAPI documentary diff proposal

No physical contract is modified.

Conceptual body-cardinality direction:

```diff
- AppID optional / transported outside body according to current physical contract
+ AppID REQUIRED in preserved InitiateDebit payload
+ semantic: canonical partnerIdentifier

  NUI REQUIRED

- ribDebiteur REQUIRED
+ ribDebiteur OPTIONAL

- nomDebiteur REQUIRED
+ nomDebiteur OPTIONAL

- devise REQUIRED
+ devise OPTIONAL

- beneficiaires REQUIRED
+ beneficiaires OPTIONAL
```

Documentary semantic additions:

```yaml
AppID:
  description: >
    Canonical SIXPAY Partner business identifier declared by the caller.
    It must correspond to the Partner resolved from the authenticated identity.

NUI:
  description: >
    Mandatory NIU used as the primary customer-resolution key.

ribDebiteur:
  description: >
    Optional account selector/control. When supplied, it must correspond to an
    account authoritatively associated with the NIU-resolved customer.
```

Documentary error semantics to be proposed at contract gate:

```text
PARTNER_IDENTITY_MISMATCH
CUSTOMER_ACCOUNT_RESOLUTION_AMBIGUOUS
ACCOUNT_NOT_FOUND_FOR_CUSTOMER
```

These names are proposal-level semantic categories only.

---

# 8. Contract-governance discrepancy

Task wording mentions:

```text
PENDING_APPROVAL / REFERENCE_ONLY / codeGenerationAllowed=false
```

but the registry observed at the selected SHA classifies
`tresorpay-payment-request-api-v1` as:

```text
ACTIVE_MVP / APPROVED / ACTIVE / true
```

Therefore INIT-0 does not claim the task-provided registry state as observed fact.

Regardless, this lot performs no generation and no physical contract edit.

A future contract change remains subject to explicit human approval because it
changes a public surface.

---

# 9. Decisions not closed by INIT-0

Still open / delegated:

- concrete Partner public resolution surface and composition strategy;
- exact mapping between Security principal and Partner storage;
- exact HTTP statuses and ProblemDetail codes;
- bank behavior when NIU returns multiple accounts;
- whether Core Banking performs authoritative account selection;
- source of currency when `devise` is absent;
- source/rule for beneficiaries when absent;
- required domain/persistence evolution enabling Payment persistence before
  account resolution.

These must not be invented by following lots.

---

# 10. Impact summary

## No modification in INIT-0

INIT-0 changes only planning documentation.

No changes to:
- Java;
- OpenAPI;
- database;
- migrations;
- security rules;
- Partner module boundaries;
- generated sources.

## Future impacted areas

```text
Payment API
Payment command/mapping
Payment initiation domain
Payment persistence state
Payment idempotency
Payment Partner isolation
Payment -> Customer Verification port
Customer Verification domain
Customer banking query/adapter
Core Banking contract usage
public Payment contract
tests / architecture tests
```

---

# 11. Exit criteria

- [x] canonical Partner identifier closed as `AppID`;
- [x] relation with trusted Security identity closed semantically;
- [x] technical idempotency target scoped by Partner;
- [x] external payment reference identity scoped by Partner;
- [x] exact known-account/RIB dependency chain inventoried;
- [x] NIU-first / optional RIB semantics closed;
- [x] Partner mismatch semantic error category documented;
- [x] ambiguous customer/account resolution semantic category documented;
- [x] OpenAPI documentary diff prepared;
- [x] no code/contract/schema/generation performed.

Status:

```text
INIT-0 — DECISION CLOSURE & IMPACT AUDIT READY FOR HUMAN REVIEW
```

---

# 12. Recommended next lot

After human review of INIT-0:

```text
INIT-2 — Partner Identity Alignment
```

must consume this document and the INIT-1 proposal before designing or
implementing any Partner/Security/Payment alignment.
