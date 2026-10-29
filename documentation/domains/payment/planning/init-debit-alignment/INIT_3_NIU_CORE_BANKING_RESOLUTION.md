# SIXPAY CONNECT — INIT-3
## NIU Core Banking Resolution

### Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- HEAD attendu: `d8ec27e108c8ab420d86947d7f4818d70ddde080`
- Mode: local implementation patch generation
- No remote Git mutation
- No gate executed by the assistant

## Goal

Make NIU the primary banking-customer resolution pivot and allow the canonical
debtor account reference to be established only after authoritative Core Banking
resolution.

The existing approved contract is reused:

`documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`

No new Core Banking endpoint is introduced.

## Effective flow

```text
Partner request
    -> AppID / Partner alignment
    -> NIU required
    -> Payment persisted without mandatory canonical debtor account
    -> BANKING_VERIFICATION_PENDING
    -> Customer Verification using NIU
       + optional debtor account hint when ribDebiteur was supplied
    -> Amplitude customer/account/KYC verification
    -> VERIFIED only
    -> canonical customerReference + accountReference
    -> downstream account-bound processing
```

## Decisions applied

- `NUI` remains required.
- `ribDebiteur` becomes optional at the Payment input/application boundary.
- `nomDebiteur` becomes optional.
- A missing RIB never causes SIXPAY to invent an account.
- The canonical account comes from authoritative Core Banking verification.
- A supplied account hint never overrides authoritative banking facts.
- No new Core Banking endpoint is invented.

## Deferred to INIT-4 / INIT-5

The following fields remain unchanged in this lot because no authoritative
source defines the replacement rule when absent:

- `devise`
- `beneficiaires`

SIXPAY must not invent a default currency or Treasury allocation.

## Persistence note

The development database may be recreated. INIT-3 does not introduce a new
table or migration because the required structural change is representable in
the existing Payment state document: the debtor account can remain unresolved
before successful banking verification.

## Exit criteria

- NIU can drive Customer Verification without a pre-known canonical account.
- Payment can enter banking verification with no resolved debtor account.
- Customer Verification may omit account-bound query fields.
- Amplitude mapping omits the account subject when none is known.
- VERIFIED Core Banking response remains the authoritative source of
  `customerReference` and `accountReference`.
- no new Core Banking endpoint is introduced.
- no default currency or Treasury allocation is invented.

Status:

```text
INIT-3 — TECHNICALLY CLOSED AT 8e5f6eb65ed412e589f970ab48a886dd219533d9
```


## Validation evidence

Observed for commit `8e5f6eb65ed412e589f970ab48a886dd219533d9`:

```text
Backend CI                 SUCCESS
Frontend CI                SUCCESS
Payment Final Validation   SUCCESS
```

The contract synchronization required by the preserved InitiateDebit wire shape is intentionally handled by INIT-4 and does not reopen INIT-3's NIU-first implementation.
