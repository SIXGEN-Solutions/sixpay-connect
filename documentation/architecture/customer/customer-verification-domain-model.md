# Customer Verification Domain Model

**Project:** SIXPAY CONNECT  
**Domain:** Customer  
**Capability:** Customer Verification  
**Phase:** 4 — Customer Verification and Observed Customer  
**Sub-lot:** 4.2.1 — Business language and invariants  
**Authoritative implementation branch:** `feat/customer-verification-observer`  
**Status:** Approved design baseline for implementation  
**Classification:** Internal engineering documentation

---

## 1. Purpose

This document fixes the ubiquitous language, semantic boundaries, outcomes,
mandatory checks, and domain invariants of the Customer Verification capability
before the creation of its Java aggregate and value objects.

Customer Verification answers one business question:

> Can SIXPAY rely on the banking system's current evidence to conclude that the
> identified customer and protected debtor-account reference are valid for the
> payment being processed?

The capability produces a canonical verification decision. It does not debit an
account, reserve funds, execute a payment, manage a subscription, or maintain an
authoritative customer master record.

---

## 2. Scope

### 2.1 In scope

Customer Verification is responsible for:

- representing a request for fresh customer and account verification;
- identifying the verification independently from a Payment identifier;
- identifying the financial institution responsible for the evidence;
- binding the request to a protected debtor-account reference;
- representing the customer identity attributes required for verification;
- evaluating the mandatory banking checks;
- distinguishing deterministic business rejection from technical uncertainty;
- producing one canonical global outcome;
- preserving the freshness and provenance of the evidence;
- producing evidence that can later be mapped to Payment without importing
  Payment implementation classes;
- preventing sensitive banking data from being exposed in domain events.

### 2.2 Out of scope

Customer Verification does not own:

- Payment state transitions;
- partner authorization;
- subscription lifecycle;
- balance and funds control;
- debit, posting, reversal, or accounting;
- Amplitude HTTP DTOs;
- retry, timeout, or circuit-breaker policies;
- persistence mapping;
- REST controllers;
- messaging listeners;
- Observed Customer projections;
- storage of raw account identifiers in events.

---

## 3. Bounded-context relationship

Customer Verification belongs to the `customer` Maven module and to the
`com.sixpay.customer.verification` capability.

It must remain independent from the Payment implementation.

```text
Payment requests banking verification
             |
             v
Customer Verification evaluates banking evidence
             |
             v
Canonical Customer Verification result
             |
             v
Application/integration mapping toward Payment evidence
```

The Customer domain must not import `com.sixpay.payment.*`.

Compatibility with Payment is semantic, not a Java package dependency.

---

## 4. Ubiquitous language

### 4.1 Customer Verification

A **Customer Verification** is a time-bound evaluation of banking evidence
concerning one customer and one protected debtor-account binding at one
financial institution.

A verification has:

- one stable verification identifier;
- one verification target;
- one financial institution;
- one protected account-binding fingerprint;
- one request time;
- one evidence observation time;
- one set of mandatory checks;
- one global outcome;
- one evidence fingerprint;
- one completion time.

A verification is not the customer itself and is not an authoritative banking
record.

### 4.2 Verification identifier

The **Customer Verification Identifier** uniquely identifies one verification
attempt.

It:

- is generated outside the domain model;
- is immutable;
- is never reused for a second attempt;
- is independent from a Payment identifier;
- is safe to expose in internal events and audit records.

Tentative Java concept:

```text
CustomerVerificationId
```

### 4.3 Verification target

The **Verification Target** groups the business subject being checked.

It contains only the information required to correlate the customer and the
protected account binding. It must not become a complete customer profile.

Tentative Java concept:

```text
CustomerVerificationSubject
```

### 4.4 Customer identity

The **Customer Identity** is the minimal normalized identity used to compare the
payment instruction with banking evidence.

It may include, according to the published contracts and banking adapter
capabilities:

- normalized NIU;
- normalized legal name or identity reference;
- another approved non-secret identity discriminator.

It must not contain credentials, secrets, authentication tokens, or unrelated
personal information.

Tentative Java concepts:

```text
CustomerIdentity
CustomerNiu
```

### 4.5 Protected account reference

The domain does not use a raw account number as its durable cross-capability
reference.

The **Account Binding Fingerprint** is an opaque, deterministic and
versioned fingerprint binding the request to the debtor account reference.

Canonical format:

```text
v1:<64 lowercase hexadecimal characters>
```

Canonical pattern:

```text
^v1:[0-9a-f]{64}$
```

The fingerprint:

- is not reversible by the domain;
- is safe for correlation but remains confidential;
- must not be treated as an account number;
- must be preserved unchanged from request to result;
- must never be recomputed from incomplete data inside the domain.

Tentative Java concept:

```text
AccountBindingFingerprint
```

### 4.6 Financial institution

The **Financial Institution Code** identifies the banking institution responsible
for the verification evidence.

It:

- is mandatory;
- is normalized;
- is stable within one verification;
- must match the institution expected by the originating payment;
- does not imply that the Customer domain owns institution configuration.

Tentative Java concept:

```text
FinancialInstitutionCode
```

### 4.7 Fresh verification

A **Fresh Verification** is based on banking evidence observed for the current
verification request and within the freshness policy accepted by the caller.

Freshness is represented by explicit timestamps and policy inputs. It must not
be inferred from the current system clock inside the domain.

Relevant times:

- `requestedAt`: when verification was requested;
- `observedAt`: when the banking evidence was observed;
- `completedAt`: when the canonical decision was completed;
- optional `validUntil`: latest instant at which the evidence remains reusable.

The domain never calls:

```java
Instant.now()
```

Time is supplied to every domain operation.

### 4.8 Verification check

A **Verification Check** is one canonical assessment performed against banking
evidence.

Each check has:

- one unique check type;
- one check result;
- an optional canonical reason code;
- no raw external message;
- no sensitive customer or account payload.

Tentative Java concept:

```text
VerificationCheck
```

### 4.9 Global outcome

The **Verification Outcome** is the canonical conclusion derived from the
complete set of mandatory checks.

Allowed values:

```text
VERIFIED
REJECTED
INDETERMINATE
```

Tentative Java concept:

```text
VerificationOutcome
```

---

## 5. Mandatory check taxonomy

The Customer Verification capability adopts the same canonical vocabulary as
the Payment evidence model.

The mandatory checks are:

| Check | Business meaning |
|---|---|
| `CUSTOMER_EXISTS` | The banking system recognizes the customer. |
| `FINANCIAL_INSTITUTION_MATCHES` | The evidence belongs to the expected institution. |
| `NIU_MATCHES` | The banking NIU matches the normalized NIU supplied for verification. |
| `IDENTITY_MATCHES` | The required customer identity attributes are consistent. |
| `ACCOUNT_EXISTS` | The protected account binding resolves to an existing account. |
| `ACCOUNT_BELONGS_TO_CUSTOMER` | The account is owned by or validly associated with the customer. |
| `ACCOUNT_IS_ACTIVE` | The account is active for banking operations. |
| `ACCOUNT_NOT_BLOCKED` | The account is not blocked. |
| `ACCOUNT_NOT_OPPOSED` | The account is not under opposition or equivalent restriction. |
| `REQUIRED_KYC_PRESENT` | Required KYC evidence exists. |
| `REQUIRED_KYC_VERIFIED` | Required KYC evidence has been verified by the bank. |

Every check type may appear at most once in one verification result.

A missing mandatory check is a malformed or incomplete evidence set. It is not
silently converted to `UNKNOWN`.

---

## 6. Check results

Allowed check results:

```text
PASS
FAIL
UNKNOWN
```

### 6.1 PASS

`PASS` means the banking evidence positively and deterministically satisfies the
check.

Rules:

- a `PASS` check has no rejection reason;
- a `PASS` check has no technical uncertainty reason;
- `PASS` never means that a check was skipped.

### 6.2 FAIL

`FAIL` means the banking evidence deterministically contradicts the required
business condition.

Examples:

- customer does not exist;
- NIU does not match;
- account does not belong to the customer;
- account is blocked;
- required KYC is absent.

Rules:

- `FAIL` is a business-negative result;
- `FAIL` requires a canonical business reason code;
- `FAIL` must never be produced merely because the bank was unavailable;
- at least one `FAIL` forces the global outcome to `REJECTED`.

### 6.3 UNKNOWN

`UNKNOWN` means no reliable conclusion can be drawn for the check.

Examples:

- technical timeout;
- unavailable banking system;
- incomplete but non-contradictory response;
- unsupported verification capability;
- evidence freshness cannot be established.

Rules:

- `UNKNOWN` is not a customer rejection;
- `UNKNOWN` must not carry a deterministic business-rejection reason;
- at least one `UNKNOWN`, with no `FAIL`, forces the global outcome to
  `INDETERMINATE`;
- infrastructure exceptions are translated into canonical uncertainty outside
  or at the boundary of the domain, never into fabricated `FAIL` values.

---

## 7. Global outcome decision table

| Check-set condition | Global outcome |
|---|---|
| All mandatory checks are present and `PASS` | `VERIFIED` |
| At least one mandatory check is `FAIL` | `REJECTED` |
| No check is `FAIL` and at least one check is `UNKNOWN` | `INDETERMINATE` |
| One or more mandatory checks are absent | Invalid evidence set; no outcome is produced |
| Duplicate check types exist | Invalid evidence set; no outcome is produced |

Decision precedence:

```text
FAIL > UNKNOWN > PASS
```

This precedence means that one deterministic business failure cannot be hidden
by technical uncertainty in another check.

---

## 8. Rejection and uncertainty reasons

Reason codes are canonical domain vocabulary. Raw Amplitude messages, stack
traces and transport errors are not domain reason codes.

### 8.1 Business rejection reasons

Recommended baseline taxonomy:

| Code | Applicable check |
|---|---|
| `CUSTOMER_NOT_FOUND` | `CUSTOMER_EXISTS` |
| `FINANCIAL_INSTITUTION_MISMATCH` | `FINANCIAL_INSTITUTION_MATCHES` |
| `NIU_MISMATCH` | `NIU_MATCHES` |
| `IDENTITY_MISMATCH` | `IDENTITY_MATCHES` |
| `ACCOUNT_NOT_FOUND` | `ACCOUNT_EXISTS` |
| `ACCOUNT_CUSTOMER_MISMATCH` | `ACCOUNT_BELONGS_TO_CUSTOMER` |
| `ACCOUNT_INACTIVE` | `ACCOUNT_IS_ACTIVE` |
| `ACCOUNT_BLOCKED` | `ACCOUNT_NOT_BLOCKED` |
| `ACCOUNT_OPPOSED` | `ACCOUNT_NOT_OPPOSED` |
| `KYC_MISSING` | `REQUIRED_KYC_PRESENT` |
| `KYC_NOT_VERIFIED` | `REQUIRED_KYC_VERIFIED` |

A business rejection reason:

- is stable and machine-readable;
- maps to exactly one failed business condition;
- must not contain personal or banking data;
- must not contain a free-form external-system message.

### 8.2 Technical indetermination reasons

Recommended baseline taxonomy:

| Code | Meaning |
|---|---|
| `BANKING_SYSTEM_UNAVAILABLE` | The authoritative banking system could not be reached. |
| `BANKING_RESPONSE_TIMEOUT` | The response did not arrive within the permitted time. |
| `BANKING_RESPONSE_INCOMPLETE` | The response lacked evidence required for a conclusion. |
| `BANKING_RESPONSE_INVALID` | The response could not be trusted or validated. |
| `CHECK_NOT_SUPPORTED` | The banking adapter cannot currently perform the check. |
| `EVIDENCE_NOT_FRESH` | Evidence freshness could not be established or accepted. |
| `TECHNICAL_RESULT_UNKNOWN` | A non-classified technical condition prevented a conclusion. |

A technical indetermination reason:

- may accompany `UNKNOWN`;
- must never accompany `FAIL`;
- must not be exposed as a customer fault;
- must remain distinct from retry and transport exception types.

Tentative Java concepts:

```text
VerificationFailureCode
VerificationIndeterminationCode
```

The implementation may use one typed reason abstraction only if it preserves the
business-versus-technical distinction at compile time.

---

## 9. Core invariants

### INV-CV-001 — Stable identity

Every verification has one non-null, immutable and unique
`CustomerVerificationId`.

### INV-CV-002 — Single target

One verification concerns exactly one customer identity and one account-binding
fingerprint at one financial institution.

### INV-CV-003 — Protected account reference

Raw account identifiers must not be present in domain events, logs generated by
domain `toString()` methods, or canonical check evidence.

### INV-CV-004 — Fingerprint format

The account-binding fingerprint follows:

```text
^v1:[0-9a-f]{64}$
```

### INV-CV-005 — Explicit time

All domain times are supplied as method parameters or constructor values.
Domain code must not call `Instant.now()` or another current-time API.

### INV-CV-006 — Complete mandatory evidence

A canonical result requires exactly one check for every mandatory check type.

### INV-CV-007 — Unique checks

A check type cannot appear more than once in a verification result.

### INV-CV-008 — Deterministic rejection

A `FAIL` represents deterministic negative banking evidence and requires a
business rejection reason.

### INV-CV-009 — Technical uncertainty

An infrastructure or availability problem produces `UNKNOWN` or prevents
completion. It never produces a fabricated `FAIL`.

### INV-CV-010 — Verified consistency

`VERIFIED` is valid only when every mandatory check is `PASS`.

### INV-CV-011 — Rejected consistency

`REJECTED` is valid only when at least one mandatory check is `FAIL`.

### INV-CV-012 — Indeterminate consistency

`INDETERMINATE` is valid only when no check is `FAIL` and at least one mandatory
check is `UNKNOWN`.

### INV-CV-013 — Immutable evidence

The canonical check collection is defensively copied, immutable and ordered by
check type.

### INV-CV-014 — Evidence provenance

A completed result records the banking source, evidence observation time and a
canonical evidence fingerprint.

### INV-CV-015 — No Payment implementation dependency

The Customer domain does not import Payment classes. Mapping to Payment evidence
is performed by a later application or integration adapter.

### INV-CV-016 — No framework dependency

Customer Verification domain classes do not depend on Spring, JPA, Servlet,
Hibernate, HTTP clients or serialization frameworks.

### INV-CV-017 — Safe events

Domain events contain identifiers, canonical enums, fingerprints and timestamps
only. They do not contain raw NIU, legal name, phone, email, account number,
credentials, access tokens or external-system payloads.

### INV-CV-018 — No silent default

Missing, duplicate or contradictory evidence is rejected as an invalid domain
input. The domain does not silently invent `PASS`, `FAIL`, or `UNKNOWN`.

---

## 10. Sensitive-data rules

### Allowed in internal domain events

- verification identifier;
- financial institution code;
- account-binding fingerprint;
- canonical check types;
- canonical check results;
- canonical reason codes;
- evidence fingerprint;
- request, observation and completion timestamps;
- correlation and causation identifiers represented by shared contracts.

### Forbidden in internal domain events

- raw bank account number;
- unmasked account identifier;
- raw NIU;
- legal name;
- telephone number;
- email address;
- identity-document number;
- JWT, API key or credential;
- raw Amplitude response;
- stack trace;
- unrestricted free-form bank message.

Where a customer correlation reference is required, it must be an approved
opaque reference or protected fingerprint.

---

## 11. Freshness rules

Freshness is a business property of the evidence, not a call to the current
clock.

A future freshness policy must evaluate explicit values such as:

```text
requestedAt
observedAt
validUntil
evaluationTime
```

Baseline invariants:

- `observedAt` must not be before the relevant banking observation;
- `completedAt` must not be before `requestedAt`;
- `validUntil`, when present, must be after or equal to `observedAt`;
- stale evidence cannot produce `VERIFIED`;
- stale evidence normally produces `UNKNOWN` with `EVIDENCE_NOT_FRESH`, unless
  the application elects to request fresh evidence before completing the domain
  decision.

The exact allowed duration is configuration or policy input and is not hardcoded
in the value object.

---

## 12. Compatibility contract with Payment

Customer Verification preserves semantic compatibility with Payment:

| Customer Verification | Payment evidence |
|---|---|
| `VerificationOutcome.VERIFIED` | `BankingVerificationOutcome.VERIFIED` |
| `VerificationOutcome.REJECTED` | `BankingVerificationOutcome.REJECTED` |
| `VerificationOutcome.INDETERMINATE` | `BankingVerificationOutcome.INDETERMINATE` |
| `VerificationCheckResult.PASS` | Payment safe check `PASS` |
| `VerificationCheckResult.FAIL` | Payment safe check `FAIL` |
| `VerificationCheckResult.UNKNOWN` | Payment safe check `UNKNOWN` |
| Customer check taxonomy | Payment `BankingVerificationCheckType` taxonomy |
| Account-binding fingerprint | Payment debtor-account binding fingerprint |
| Customer evidence fingerprint | Payment evidence fingerprint |

This table is a semantic compatibility requirement. It does not authorize Java
imports from Payment into Customer.

A future compatibility test should compare the enum names and mandatory check
sets through test fixtures or a published shared contract.

---

## 13. Candidate domain types for subsequent sub-lots

This document does not create implementation classes. It reserves the following
vocabulary for sub-lots 4.2.2 to 4.2.4:

```text
CustomerVerification
CustomerVerificationId
CustomerVerificationStatus
CustomerVerificationRequest
CustomerVerificationSubject
CustomerIdentity
CustomerNiu
FinancialInstitutionCode
AccountBindingFingerprint
VerificationCheck
VerificationCheckType
VerificationCheckResult
VerificationOutcome
VerificationFailureCode
VerificationIndeterminationCode
VerificationEvidenceFingerprint
CustomerVerificationDomainEvent
CustomerVerificationCompleted
RequiredVerificationChecksPolicy
VerificationOutcomePolicy
```

Names may only change if an existing shared-kernel type is found to own exactly
the same semantics.

---

## 14. Decision summary

The following decisions are frozen for implementation:

1. Customer Verification is a dedicated capability inside the `customer`
   module.
2. It produces a canonical, time-bound banking verification decision.
3. Global outcomes are exactly `VERIFIED`, `REJECTED`, and `INDETERMINATE`.
4. Check results are exactly `PASS`, `FAIL`, and `UNKNOWN`.
5. The eleven Payment banking-check names are adopted as the canonical check
   vocabulary.
6. A deterministic negative answer produces `FAIL` and therefore `REJECTED`.
7. A technical inability to conclude produces `UNKNOWN` and therefore
   `INDETERMINATE`, never a fabricated rejection.
8. All mandatory checks must be present exactly once.
9. Raw sensitive customer and account data is forbidden in domain events.
10. Time is supplied explicitly; domain code never obtains the current time.
11. Customer does not import Payment implementation classes.
12. The account is represented across capability boundaries by a versioned,
    protected account-binding fingerprint.
13. Evidence freshness and provenance are explicit domain concepts.
14. Mapping to Payment belongs to a later application/integration layer.

---

## 15. Exit criteria

Sub-lot 4.2.1 is complete when:

- this vocabulary is accepted as the implementation baseline;
- no conflicting term remains between Customer Verification and Payment;
- business rejection and technical indetermination are unambiguously distinct;
- mandatory checks and decision rules are frozen;
- sensitive-data and explicit-time rules are frozen;
- subsequent Java classes can be implemented without inventing new semantics.
