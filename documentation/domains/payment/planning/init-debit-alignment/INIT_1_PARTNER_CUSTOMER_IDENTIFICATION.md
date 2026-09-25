# SIXPAY CONNECT — INIT-1
## Partner & Customer Identification Contract Proposal

### Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- SHA: `8c0c02d65f2d9ed10c143702b7a13cd9bdcbe94d`
- Mode: contract-change proposal only
- Physical OpenAPI changed: no
- Contract registry changed: no
- Java changed: no
- Code generation: no
- Commit/push: no

---

# 1. Purpose

Prepare the future contract change for `InitiateDebit` by reconciling the
current physical Payment contract with the decisions already closed in INIT-0.

This document is a proposal artifact. It does not itself authorize a public
contract change.

The current authoritative physical contract remains:

`documentation/contracts/tresorpay/tresorpay-payment-request-api-v1.yaml`

The current authoritative registry remains:

`documentation/contracts/CONTRACT_REGISTRY.yaml`

---

# 2. Inputs carried forward from INIT-0

The following decisions are treated as closed inputs:

```text
AppID == canonical business partnerIdentifier
NUI == required NIU pivot
ribDebiteur == OPTIONAL
nomDebiteur == OPTIONAL
devise == OPTIONAL
beneficiaires == OPTIONAL
```

Additional closed semantics:

```text
technical idempotency scope
    = (partnerIdentifier, operation, idempotencyKey)

business external payment identity
    = (partnerIdentifier, externalPaymentReference)

externalPaymentReference
    = endToEndId
```

Partner identity rule:

```text
Security-authenticated identity
        ↓
resolve Partner
        ↓
canonical partnerIdentifier
        ↓
must equal
        ↓
request AppID
```

Customer/account rule:

```text
NUI first
  ↓
resolve banking customer
  ↓
resolve eligible account(s)
  ↓
optional RIB is only a selector/control
```

---

# 3. Current contract divergences

The current physical contract is materially different from the preserved
`InitiateDebit` payload.

## 3.1 Partner identity transport

Current contract:

```text
AppID -> X-TresorPay-App-Id header
acceptedInBody: false
```

Target proposal:

```text
AppID remains in JSON body
AppID is REQUIRED
AppID semantic = canonical partnerIdentifier
```

The header may not be retained as a second independent Partner business
identity. If compatibility requires a header temporarily, the contract must
state that the values are identical and that one is authoritative.

INIT-1 does not choose a duplicate-identity compatibility strategy silently.

---

## 3.2 LoginName

Current contract:

```text
LoginName forbidden in request body
mapped to Bearer subject / registered client identity
```

Target proposal:

```text
LoginName remains REQUIRED in the preserved payload
```

However:

- `LoginName` is not the canonical Partner business identifier;
- it must not replace Security authentication;
- it must not be used as the durable multi-partner ownership key.

---

## 3.3 External payment reference

Current normalized field:

```text
tresorPayPaymentReference
```

Preserved payload field:

```text
endToEndId
```

Target semantics:

```text
externalPaymentReference = endToEndId
business uniqueness scope = (partnerIdentifier, endToEndId)
```

The contract proposal should preserve the actual payload field name when the
contract is amended.

---

## 3.4 Customer identity

Current normalized schema:

```text
customer.niu
customer.name
```

Preserved payload:

```text
NUI
nomDebiteur
```

Target:

```text
NUI REQUIRED
nomDebiteur OPTIONAL
```

The NIU is the primary customer-resolution key.
`nomDebiteur`, if supplied, is non-authoritative descriptive input.

---

## 3.5 Debtor account

Current contract:

```text
debtorAccount REQUIRED
AccountReference requires at least one of:
- rib
- iban
- opaqueAccountReference
```

Target:

```text
ribDebiteur OPTIONAL
no new RIP field
no new accountNumber field
```

Absence of `ribDebiteur` must not fail request validation before NIU-first
customer/account resolution.

A supplied RIB is only a selector/control against authoritative banking
accounts.

---

## 3.6 Currency

Current contract nests currency under:

```text
totalAmount.currency REQUIRED
```

Preserved payload separates:

```text
montantTotal REQUIRED
devise OPTIONAL
```

Target proposal:

```text
montantTotal REQUIRED
devise OPTIONAL
```

No default currency is invented by INIT-1.
The source of an absent currency remains a separate decision before runtime
implementation if a non-null currency is required by downstream policy.

---

## 3.7 Beneficiaries

Current contract:

```text
treasuryBeneficiaries REQUIRED
minItems: 1
```

Target:

```text
beneficiaires OPTIONAL
```

INIT-1 does not invent treasury allocation when absent.

If a later authoritative rule derives beneficiaries from protected
configuration, that behavior must be defined before implementation.

---

## 3.8 Callback

Current normalized field:

```text
callbackUrl
```

Preserved field:

```text
callbackURL
```

Target remains REQUIRED.

Existing HTTPS/allow-list security constraints may be preserved unless a
separate approved decision changes them.

---

# 4. Proposed future request shape

The future public payload should be prepared around the existing field names:

```yaml
type: object
additionalProperties: false
required:
  - LoginName
  - AppID
  - endToEndId
  - montantTotal
  - typeCreance
  - NUI
  - dateExecution
  - callbackURL
properties:
  LoginName:
    type: string
    minLength: 1
    maxLength: 64

  AppID:
    type: string
    minLength: 1
    maxLength: 64
    description: >
      Canonical SIXPAY Partner business identifier declared by the caller.
      It must match the Partner resolved from the authenticated SIXPAY identity.

  endToEndId:
    type: string
    minLength: 1
    maxLength: 128
    description: >
      External payment reference. Business uniqueness is scoped by AppID.

  montantTotal:
    type: number
    exclusiveMinimum: 0
    multipleOf: 0.01

  devise:
    type: string
    pattern: '^[A-Z]{3}$'
    description: >
      Optional currency. No defaulting rule is defined by INIT-1.

  ribDebiteur:
    type: string
    minLength: 8
    maxLength: 64
    description: >
      Optional debtor-account selector/control. When supplied, it must resolve
      to an account belonging to the customer authoritatively resolved by NUI.

  nomDebiteur:
    type: string
    maxLength: 200
    description: >
      Optional non-authoritative debtor name supplied by the Partner.

  typeCreance:
    type: string

  NUI:
    type: string
    minLength: 1
    maxLength: 64
    description: >
      Mandatory NIU used as the primary banking customer-resolution key.

  dateExecution:
    type: string
    format: date-time

  beneficiaires:
    type: array
    maxItems: 20
    description: >
      Optional beneficiary allocation. INIT-1 defines no automatic allocation
      when absent.

  callbackURL:
    type: string
    format: uri
    maxLength: 2048
    pattern: '^https://'
```

This is a documentary proposal, not an applied OpenAPI schema.

---

# 5. Proposed identity and authorization semantics

## Partner

```text
authenticated SIXPAY principal
        ↓
Partner resolution
        ↓
partnerIdentifier
        ↓
compare with AppID
```

Target semantic failures:

```text
PARTNER_NOT_RESOLVED
PARTNER_IDENTITY_MISMATCH
PARTNER_NOT_AUTHORIZED
```

Only `PARTNER_IDENTITY_MISMATCH` was explicitly closed as a proposal-level
category in INIT-0. The other labels remain descriptive placeholders and must
not become public codes without approval.

## Customer/account

```text
NUI
  ↓
customer resolution
  ↓
eligible accounts
  ↓
0 account            -> functional rejection
1 account            -> use authoritative account
>1 accounts + RIB    -> select/control by RIB
>1 accounts no RIB   -> ambiguous unless bank authoritatively selects one
```

Proposal-level semantic categories:

```text
CUSTOMER_NOT_FOUND
ACCOUNT_NOT_FOUND_FOR_CUSTOMER
CUSTOMER_ACCOUNT_RESOLUTION_AMBIGUOUS
```

Exact HTTP statuses and stable `ProblemDetail.code` values require contract
approval.

---

# 6. Idempotency proposal

The current physical contract scopes idempotency with TRESOR PAY/client/header
semantics.

Target multi-partner scope:

```text
partnerIdentifier
operation
idempotencyKey
```

For initiation:

```text
partnerIdentifier = AppID
operation = PAYMENT_INITIATE_DEBIT
idempotencyKey = Idempotency-Key
```

Canonical fingerprint should include the canonical Partner business identity and
the canonical business payload, while excluding transport-only metadata such as
the idempotency key itself and correlation ID.

---

# 7. External payment reference proposal

The current contract treats the TRESOR PAY reference as globally unique.

Target:

```text
(partnerIdentifier, externalPaymentReference)
```

with:

```text
partnerIdentifier = AppID
externalPaymentReference = endToEndId
```

Therefore conflict/replay semantics must be evaluated within the Partner scope.

This proposal does not change persistence or unique constraints.

---

# 8. Security contract impact

The existing physical contract binds JWT claims to:

```text
X-TresorPay-App-Id
debtor_account_hash
payment_reference
```

The target decisions affect this model because:

- `AppID` returns to the body;
- `ribDebiteur` becomes optional;
- an account may not exist yet at intake time;
- NIU becomes the first banking resolution pivot.

Therefore a mandatory pre-intake `debtor_account_hash` binding is incompatible
with requests that intentionally omit RIB unless the Security profile defines a
different authoritative binding.

INIT-1 does not alter the signed-token security profile.
This is a required Security decision before a physical contract amendment can
be approved.

---

# 9. Current contract rules that conflict with target decisions

The following current rules cannot be copied unchanged into the future contract:

```text
LoginName forbidden from body
AppID accepted only as X-TresorPay-App-Id
debtorAccount REQUIRED
customer.name REQUIRED
totalAmount.currency REQUIRED
treasuryBeneficiaries REQUIRED
debtor_account_hash required in token binding
global wording around TRESOR PAY payment-reference uniqueness
```

The following concepts remain compatible in principle:

```text
Idempotency-Key required
X-Correlation-ID required
HTTPS callback restriction
safe ProblemDetail responses
no credentials in logs/audit
replay returns same logical result
same key + changed business payload => conflict
blind retry with a new key forbidden
```

---

# 10. Physical contract change strategy

Recommended future amendment sequence:

```text
1. approve request payload shape
2. approve Partner identity/security binding
3. approve NIU-first account-resolution semantics
4. approve error codes/statuses
5. approve idempotency/reference scoping
6. update physical OpenAPI
7. update CONTRACT_REGISTRY if lifecycle/approval/generation metadata changes
8. validate OpenAPI and documentation
9. only then authorize implementation/generation
```

INIT-1 stops before step 6.

---

# 11. Files proposed for a future physical contract change

Potentially impacted:

```text
documentation/contracts/tresorpay/tresorpay-payment-request-api-v1.yaml
documentation/contracts/CONTRACT_REGISTRY.yaml
documentation/domains/payment/planning/init-debit-alignment/README.md
```

Potential follow-on implementation areas are already inventoried by INIT-0 and
are intentionally excluded from INIT-1.

---

# 12. Open decisions blocking physical OpenAPI amendment

The physical contract must not yet be rewritten until the following are closed:

1. whether the Partner identity remains duplicated in both body `AppID` and a
   transport header for compatibility, or body `AppID` becomes the only
   declared business identifier;
2. exact Security binding after `ribDebiteur` becomes optional;
3. behavior when a NIU resolves several eligible accounts and no RIB is supplied;
4. exact public error/status mapping;
5. authoritative source of currency when `devise` is absent;
6. behavior/source of beneficiaries when `beneficiaires` is absent.

---

# 13. Governance observed at this revision

At SHA `8c0c02d65f2d9ed10c143702b7a13cd9bdcbe94d`,
the physical contract embeds:

```text
lifecycleStatus: ACTIVE_MVP
approvalStatus: APPROVED
generationPolicy: ACTIVE
codeGenerationAllowed: true
```

The registry is authoritative and must be re-checked at the moment a physical
contract amendment is authorized.

INIT-1 itself performs no generation.

---

# 14. Exit criteria

- [x] existing physical contract reviewed;
- [x] INIT-0 decisions carried forward;
- [x] body/header Partner identity conflict identified;
- [x] preserved payload proposal documented;
- [x] NIU-first / optional-RIB contract impact documented;
- [x] idempotency scope documented;
- [x] external-reference scope documented;
- [x] Security binding conflict documented;
- [x] future contract files identified;
- [x] no physical contract changed;
- [x] no generation executed.

Status:

```text
INIT-1 — CONTRACT CHANGE PROPOSAL READY FOR HUMAN REVIEW
```
