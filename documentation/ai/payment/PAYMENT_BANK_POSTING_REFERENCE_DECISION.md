# SIXPAY CONNECT — BankPostingReference Decision

> **Decision ID:** `PAY-DEC-IA1-001`  
> **Gate:** `IA-1 — PAYMENT DOMAIN BRIEF`  
> **Lot:** `1 — Ubiquitous Language and Domain Boundaries`  
> **Status:** `DECIDED_PENDING_EXTERNAL_CONTRACT_APPROVAL`  
> **Code generation:** **FORBIDDEN**

## 1. Decision

`BankPostingReference` is a composite abstraction:

```text
principalPostingReference   mandatory when posting is confirmed
debitLegReference           optional
cutCreditLegReference       optional
```

The principal reference identifies the atomic posting as a whole. Leg
references are retained only when Amplitude supplies stable identifiers.

## 2. Rationale

The preferred bank operation is atomic debit plus CUT credit with one stable
posting identity, while the contract also anticipates leg-level outcomes.

A scalar-only model would lose useful audit information. Two mandatory leg
references would assume capabilities the bank has not yet approved.

## 3. Invariants

| ID | Invariant |
| --- | --- |
| `PAY-POSTREF-001` | Confirmed posting has a non-blank principal reference. |
| `PAY-POSTREF-002` | Principal reference is stable and immutable. |
| `PAY-POSTREF-003` | Leg references are optional and immutable when present. |
| `PAY-POSTREF-004` | Leg references never replace the principal reference. |
| `PAY-POSTREF-005` | References contain no account number or credential. |
| `PAY-POSTREF-006` | Unknown lookup uses original idempotency evidence until a bank reference is known. |
| `PAY-POSTREF-007` | Reversal has its own reference and never overwrites posting reference. |
| `PAY-POSTREF-008` | TFJ matching uses the principal reference unless an approved contract states otherwise. |

## 4. Conceptual boundary

`BankPostingReference` contains identifiers only. Status, timestamps, amounts,
reversal reference and raw Amplitude response belong elsewhere.

## 5. External confirmation still required

Amplitude and Accounting must confirm:

- whether one principal reference is exposed;
- whether debit and CUT leg references are exposed;
- uniqueness scope;
- authoritative lookup and TFJ matching identifier.

No implementation may make leg references mandatory before approval.
