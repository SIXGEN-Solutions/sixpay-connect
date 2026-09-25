# SIXPAY CONNECT — INIT-5
## Payment Input & NIU Resolution Implementation

### Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- Baseline SHA: `b11d4f3ace2d9f304af6e0c973885f569511dc80`
- Mode: local implementation patch preparation
- Remote Git mutation: **NO**
- Gates executed by assistant: **NO**

## Purpose

Align the Payment input/application boundary with INIT-4 and complete the
runtime NIU-first resolution path.

## Scope

- align `InitiateDebitRequest`;
- align `InitiateDebitCommand`;
- align `PaymentCommandApiMapper`;
- remove old mandatory RIB/name/currency/beneficiary intake assumptions where
  INIT-4 made those fields optional;
- keep `AppID` mandatory;
- keep NIU mandatory;
- build Customer Verification context without requiring a pre-resolved account;
- allow debtor legal name to be absent;
- propagate authoritative verified account context back to Payment;
- construct `DebtorAccountReference` only after VERIFIED banking evidence.

## Explicit non-invention rule

No default currency and no automatic Treasury beneficiary allocation is
introduced by INIT-5.

If downstream construction still requires `Money` or
`TreasuryAllocationIntent`, absence of `devise` or `beneficiaires` must remain
visible and must be resolved only by an existing authoritative rule or a later
approved decision.

## Account resolution flow

```text
InitiateDebit
  -> NUI required
  -> Payment persisted without canonical debtor account
  -> BANKING_VERIFICATION_PENDING
  -> Customer Verification by NIU
  -> authoritative VERIFIED bank result
  -> customerReference + accountReference + bank account metadata
  -> build canonical DebtorAccountReference
  -> persist on Payment
  -> downstream account-bound processing
```

## Status

```text
INIT-5 — IMPLEMENTATION PATCH PREPARED; VALIDATION REQUIRED
```
