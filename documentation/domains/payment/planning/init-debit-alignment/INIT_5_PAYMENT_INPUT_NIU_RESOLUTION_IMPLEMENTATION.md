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

`devise` remains optional, but an omitted value defaults to `XAF` for the
current MVP deployment. No automatic Treasury beneficiary allocation is
introduced by INIT-5.

If `devise` is omitted, the application boundary applies the approved MVP
`XAF` default. If a syntactically valid ISO currency is supplied, SIXPAY
propagates it and Core Banking remains authoritative for banking validation.
Absence of `beneficiaires` remains visible and must not trigger an invented
Treasury allocation.

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
INIT-5 — TECHNICALLY CLOSED
```


## INIT-7 closure note

Final alignment confirmed that the public request DTO, API mapper and NIU-first
Customer Verification flow are implemented.

INIT-7 clarified the approved MVP currency rule: `devise` remains optional at
the wire boundary, but an omitted value defaults to `XAF`. A supplied
syntactically valid ISO currency is propagated; authoritative banking currency
validation belongs to Core Banking.

`beneficiaires` remains optional at the wire boundary. No automatic Treasury
beneficiary allocation is introduced by INIT-5/INIT-7.
