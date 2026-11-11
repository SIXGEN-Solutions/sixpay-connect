# INIT-7 — Init Debit Final Alignment & Closure

## Status

CLOSURE_PREPARED_PENDING_HUMAN_VALIDATION

Reference baseline: `d8657a05bfc57e06ed1705fbcb70d43e9220c549`

## Purpose

Close the `init-debit-alignment` workstream by reconciling the current physical
Payment request contract, implementation, persistence, idempotency and recovery
semantics with INIT-0 through INIT-6.

## Final alignment findings

### Public payload

The physical `PaymentRequest` contract and `InitiateDebitRequest` agree on:

- `LoginName`: required compatibility metadata, not authentication;
- `AppID`: required canonical Partner business identifier;
- `endToEndId`: required Partner-scoped external Payment reference;
- `montantTotal`: required;
- `devise`: optional; defaults to `XAF` when omitted for the current MVP;
- `ribDebiteur`: optional;
- `nomDebiteur`: optional;
- `typeCreance`: required;
- `NUI`: required NIU pivot;
- `dateExecution`: required;
- `beneficiaires`: optional;
- `callbackURL`: required HTTPS callback.

No new JSON `partnerIdentifier`, `RIP` or `accountNumber` field is introduced.

### Partner identity

The business identity invariant remains:

```text
authenticated machine subject
    -> Security machine-identity link
    -> registered Partner
    -> Partner.partnerIdentifier
    == request.AppID
```

`LoginName` remains non-authenticating compatibility metadata.
`X-TresorPay-App-Id` remains independent transport metadata.

### NIU-first resolution

Payment can enter banking verification without a canonical debtor account.
`NUI` is the mandatory banking-customer pivot. Optional `ribDebiteur` is only a
selector/control and cannot override authoritative Core Banking facts.

Authoritative VERIFIED banking evidence supplies the canonical customer/account
references required for downstream processing.

### Optional currency and beneficiaries

The public contract leaves `devise` and `beneficiaires` optional.

For the current MVP, an omitted `devise` defaults to `XAF`, reflecting the
initial Central African financial deployment context. A supplied syntactically
valid ISO alpha-3 currency is propagated by SIXPAY; authoritative banking
currency validation belongs to Core Banking.

Making the default currency environment-configurable is explicitly deferred to
a future evolution.

No automatic Treasury beneficiary allocation is introduced.

### Idempotency / persistence / recovery

Initiation idempotency is Partner-scoped:

```text
(partnerIdentifier, operation, idempotencyKey)
```

External Payment identity is Partner-scoped:

```text
(partnerIdentifier, externalPaymentReference)
```

Same Partner + same external reference + same canonical fingerprint replays the
completed acknowledgement.

Same Partner + same external reference + different fingerprint follows the
Payment-reference conflict path.

`GET Payment` remains read-only recovery and never triggers banking
verification, OTP creation/resend, posting or financial replay.

### INIT-6 validation

The human operator reported the requested INIT-6 gates completed successfully
with exit code `0`. INIT-6 is therefore classified `TECHNICALLY_CLOSED`.

## Workstream closure

After the INIT-7 correction and successful validation, the intended final lot
statuses are:

```text
INIT-0  COMPLETED-PROPOSAL
INIT-1  COMPLETED-PROPOSAL
INIT-2  COMPLETED
INIT-3  TECHNICALLY-CLOSED
INIT-4  TECHNICALLY-CLOSED
INIT-5  TECHNICALLY-CLOSED
INIT-6  TECHNICALLY-CLOSED
INIT-7  TECHNICALLY-CLOSED
```

## Explicitly separate workstreams

The following subjects are not part of Init Debit closure and must not reopen
this workstream unless they change an approved Init Debit contract or invariant:

- human user authentication evolution, including LDAP;
- Local/OIDC/LDAP identity convergence and user identity linking;
- Partner-neutrality / removal of provider-specific terminology;
- later Payment execution/funds-control/posting capabilities;
- Accounting/T1 evolution.

LDAP concerns belong to the Security authentication workstream. They are
distinct from the Partner machine-identity invariant used by Initiate Debit.

## Validation required

The assistant does not execute gates for INIT-7.

Targeted:

```bash
cd backend
mvn -pl payment,bootstrap -am test
```

Then:

```bash
mvn verify
```

Repository gates from the repository root:

```bash
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
py scripts/verify_documentation_final.py
py scripts/verify_baseline.py
```

After all applicable commands finish with exit code `0`, INIT-7 and the
`init-debit-alignment` workstream may be marked technically closed.
