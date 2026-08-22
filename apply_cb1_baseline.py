#!/usr/bin/env python3
from pathlib import Path

ROOT = Path.cwd()

def read(path):
    p = ROOT / path
    if not p.exists():
        raise SystemExit(f"Missing expected file: {path}")
    return p.read_text(encoding="utf-8")

def write(path, content):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8", newline="\n")

def replace_once(text, old, new, label):
    if new in text:
        print(f"[skip] {label}: already applied")
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"[stop] {label}: expected exactly one match, found {count}")
    print(f"[ok]   {label}")
    return text.replace(old, new, 1)

def replace_in_contract(text, contract_id, old, new, label):
    marker = f'  - id: "{contract_id}"'
    start = text.find(marker)
    if start < 0:
        raise SystemExit(f"[stop] contract not found: {contract_id}")
    next_start = text.find('\n  - id: "', start + len(marker))
    end = len(text) if next_start < 0 else next_start
    section = text[start:end]
    updated = replace_once(section, old, new, label)
    return text[:start] + updated + text[end:]

# CONTRACT_REGISTRY.yaml
path = "documentation/contracts/CONTRACT_REGISTRY.yaml"
text = read(path)
text = replace_once(
    text,
    '  branch: "feat/payment-contract-pack"',
    '  branch: "feat/sixpay-customer-management-baseline"',
    "registry branch",
)
text = replace_once(
    text,
    '    PENDING_APPROVAL: "Technically prepared and awaiting formal approval."\n'
    '    APPROVED: "Approved by the required authorities."',
    '    PENDING_APPROVAL: "Technically prepared and awaiting formal approval."\n'
    '    PENDING_BANK_APPROVAL: "SIXPAY baseline prepared; authoritative bank/provider approval is still required."\n'
    '    APPROVED: "Approved by the required authorities."',
    "approval model",
)
text = replace_in_contract(
    text,
    "amplitude-customer-verification-api-v1",
    '    approvalStatus: "PENDING_APPROVAL"',
    '    approvalStatus: "PENDING_BANK_APPROVAL"',
    "customer verification approval",
)
text = replace_in_contract(
    text,
    "amplitude-customer-verification-api-v1",
    '        - "Does not authorize or manage a TRESOR PAY subscription"',
    '        - "Does not authorize or manage a SIXPAY Customer subscription"',
    "customer verification subscription wording",
)
text = replace_in_contract(
    text,
    "amplitude-payment-posting-api-v1",
    '    approvalStatus: "PENDING_APPROVAL"',
    '    approvalStatus: "PENDING_BANK_APPROVAL"',
    "payment posting approval",
)
text = replace_in_contract(
    text,
    "amplitude-payment-posting-api-v1",
    '      relationshipToVerificationContract: >\n'
    '        Execution-time checks are defense in depth and do not replace the\n'
    '        dedicated pre-posting amplitude-payment-verification-api-v1 contract\n'
    '        identified by PAY-CONTRACT-02.',
    '      relationshipToCustomerVerificationContract: >\n'
    '        Execution-time checks are defense in depth and do not replace the\n'
    '        dedicated customer/account/KYC verification defined by\n'
    '        amplitude-customer-verification-api-v1.',
    "obsolete payment-verification contract reference",
)
text = replace_in_contract(
    text,
    "amplitude-end-of-day-confirmation-api-v1",
    '    approvalStatus: "PENDING_APPROVAL"',
    '    approvalStatus: "PENDING_BANK_APPROVAL"',
    "EOD approval",
)
write(path, text)

# amplitude-customer-verification-api-v1.yaml
path = "documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml"
text = read(path)
text = replace_once(
    text,
    '    approvalStatus: "PENDING_APPROVAL"',
    '    approvalStatus: "PENDING_BANK_APPROVAL"',
    "customer OpenAPI approval",
)
text = replace_once(
    text,
    '      fundsControlContract: "amplitude-payment-verification-api-v1"',
    '      fundsControlContract: "amplitude-payment-posting-api-v1"',
    "customer OpenAPI funds contract reference",
)
text = replace_once(
    text,
    '    Amplitude remains the system of record. The contract exposes normalized\n'
    '    facts and verification evidence used by the Payment workflow. It does not\n'
    '    authorize or manage a TRESOR PAY subscription: TRESOR PAY remains the\n'
    '    subscription system of record for the MVP.',
    '    Amplitude remains the system of record for banking customer/account facts.\n'
    '    The contract exposes normalized facts and verification evidence used by\n'
    '    Customer Management and Payment. It does not authorize or manage a SIXPAY\n'
    '    subscription. SIXPAY owns its enrolled Customer and partner-subscription\n'
    '    lifecycle.',
    "customer OpenAPI system-of-record wording",
)
write(path, text)

# synchronous-integration-flows.md
path = "documentation/architecture/integration/synchronous-integration-flows.md"
text = read(path)

pairs = [
(
'''Payment
  -> VerificationGateway
  -> AmplitudeVerificationAdapter
  -> AmplitudeBankingClient
  -> Core Banking''',
'''Payment
  -> VerificationGateway
  -> AmplitudeVerificationAdapter
  -> capability-specific Amplitude account/funds client
  -> Core Banking''',
"SYN-04 target client",
),
(
'The port and adapter exist. The real `AmplitudeBankingClient` implementation does not.',
'''The legacy `AmplitudeBankingClient` interface remains as foundation debt but is
not the target design. Current integration work uses narrow capability-specific
clients/adapters. Customer/KYC/account verification remains owned by Customer;
execution-time account/funds checks remain owned by Payment.''',
"SYN-04 current state",
),
(
'''Payment
  -> FundsGateway
  -> AmplitudeFundsAdapter
  -> AmplitudeBankingClient
  -> Core Banking''',
'''Payment
  -> FundsGateway
  -> AmplitudeFundsAdapter
  -> AmplitudeAccountFundsClient
  -> Core Banking''',
"SYN-05 funds client",
),
(
'''Payment
  -> PostingGateway
  -> AmplitudePostingAdapter
  -> AmplitudeBankingClient.postPayment
  -> Core Banking''',
'''Payment
  -> PostingGateway
  -> DedicatedAmplitudePostingAdapter
  -> AmplitudePostingClient
  -> RestAmplitudePostingClient
  -> Core Banking''',
"SYN-06 posting client",
),
(
'''Payment reconciliation
  -> AmplitudeLookupAdapter
  -> find by idempotency key or bank reference
  -> resolve success / rejection / still unknown''',
'''Payment reconciliation
  -> capability-specific posting lookup adapter/client
  -> lookup by original idempotency key or bank reference
  -> resolve success / rejection / still unknown''',
"SYN-07 lookup",
),
(
'''Authorized compensation
  -> ReversalGateway
  -> AmplitudeReversalAdapter
  -> Core Banking reversal''',
'''Authorized compensation
  -> ReversalGateway
  -> capability-specific Amplitude reversal adapter/client
  -> Core Banking reversal''',
"SYN-08 reversal",
),
]

for old, new, label in pairs:
    text = replace_once(text, old, new, label)

text = replace_once(
    text,
    '''- complete audit.

## 10. Internal query flows''',
    '''- complete audit.

Reversal is an optional capability until the bank explicitly confirms support,
semantics and operational controls.

## 10. Core Banking contract status

The authoritative CB-1 status matrix and bank-confirmation gate are maintained
in:

`documentation/architecture/integration/core-banking-api-baseline.md`

## 11. Internal query flows''',
    "CB-1 documentation link",
)
write(path, text)

baseline = '''# CB-1 — Core Banking API Baseline

## Purpose

This document is the authoritative architecture baseline for Core Banking APIs
consumed by SIXPAY CONNECT on `feat/sixpay-customer-management-baseline`.

No API is bank-approved solely because an OpenAPI contract or Java adapter
exists. External approval is a separate gate.

## Status model

### Bank approval

- `APPROVED`: formally approved by the bank/provider.
- `PENDING_BANK_APPROVAL`: SIXPAY baseline exists; bank approval is missing.
- `TO_DEFINE`: no stable baseline exists.

### MVP requirement

- `REQUIRED`: required by the target MVP.
- `OPTIONAL`: used only if the bank confirms support and the project enables it.

`OPTIONAL` is not an approval status.

## Inventory

| Capability | Contract | Owner | Bank approval | MVP |
|---|---|---|---|---|
| Customer discovery | `amplitude-customer-verification-api-v1.yaml` | Customer | `PENDING_BANK_APPROVAL` | `REQUIRED` |
| Customer/KYC/account verification | `amplitude-customer-verification-api-v1.yaml` | Customer | `PENDING_BANK_APPROVAL` | `REQUIRED` |
| Payment execution/funds check | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_BANK_APPROVAL` | `REQUIRED` |
| Fund reservation/lookup/release | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_BANK_APPROVAL` | `OPTIONAL` |
| Atomic debit + CUT credit posting | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_BANK_APPROVAL` | `REQUIRED` |
| Posting lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_BANK_APPROVAL` | `REQUIRED` |
| Reversal + reversal lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_BANK_APPROVAL` | `OPTIONAL` |
| TFJ/EOD callback + fallback lookup | `amplitude-end-of-day-confirmation-api-v1.yaml` | Accounting / Payment lifecycle | `PENDING_BANK_APPROVAL` | `REQUIRED` |

## Signatures to confirm with the bank

### Customer verification

`POST /api/v1/customer-verifications`

Customer discovery:
- `GET /api/v1/customers`
- `GET /api/v1/customers/{customerReference}`
- `GET /api/v1/customers/{customerReference}/accounts`

### Funds check

`POST /api/v1/payment-checks`

### Posting

`POST /api/v1/payment-postings`

A financial command with an uncertain transport outcome is never blindly retried.

### Posting lookup

- `GET /api/v1/payment-posting-lookups/{idempotencyKey}`
- `GET /api/v1/payment-postings/{bankPostingReference}`

At least one authoritative lookup mechanism is mandatory before posting sandbox certification.

### Reversal

- `POST /api/v1/payment-postings/{bankPostingReference}/reversals`
- `GET /api/v1/payment-postings/{bankPostingReference}/reversals/{reversalReference}`

Reversal is `OPTIONAL` until explicitly confirmed by the bank.

### TFJ / EOD

Primary proposal:
`POST <SIXPAY>/webhooks/v1/amplitude/end-of-day-confirmations`

Fallback proposal:
`GET <AMPLITUDE>/api/v1/end-of-day-confirmations`

The bank must confirm whether the real mechanism is callback, query, file/batch,
or another integration mechanism.

## Resolved documentary drift

1. `PENDING_BANK_APPROVAL` is the canonical Amplitude status while provider approval is missing.
2. `OPTIONAL` describes MVP requirement, not approval.
3. `amplitude-payment-verification-api-v1` is obsolete/nonexistent; execution-time funds checking belongs to `amplitude-payment-posting-api-v1.yaml`.
4. `AmplitudeBankingClient` is legacy foundation debt; narrow capability-specific clients are the target.
5. Amplitude owns banking facts; SIXPAY owns SIXPAY Customer enrollment and partner-subscription lifecycle.

## Promotion to APPROVED

Promotion requires traceable bank evidence for endpoint/method, request/response
schema, security, error/status codes, idempotency/retry semantics where
applicable, sandbox URL/certificates, approval date and reference.
'''
new1 = ROOT / "documentation/architecture/integration/core-banking-api-baseline.md"
if not new1.exists():
    write(str(new1.relative_to(ROOT)), baseline)
    print("[ok]   created core-banking-api-baseline.md")
else:
    print("[skip] core-banking-api-baseline.md already exists")

checklist = '''# CB-1 — Amplitude Bank Signature Confirmation Checklist

All items remain `PENDING_BANK_APPROVAL` until traceable bank/provider evidence exists.

## Customer verification
- [ ] Confirm `POST /api/v1/customer-verifications`.
- [ ] Confirm request/response fields and identifiers.
- [ ] Confirm NIU/KYC semantics.
- [ ] Confirm business-negative vs technical-error mapping.
- [ ] Confirm OAuth2 token URL, scopes/audience and mTLS.
- [ ] Confirm timeout, rate-limit and freshness rules.

## Customer discovery
- [ ] Confirm `GET /api/v1/customers`.
- [ ] Confirm `GET /api/v1/customers/{customerReference}`.
- [ ] Confirm `GET /api/v1/customers/{customerReference}/accounts`.
- [ ] Confirm supported search keys and masking/persistence rules.

## Funds check
- [ ] Confirm `POST /api/v1/payment-checks`.
- [ ] Confirm available vs ledger balance.
- [ ] Confirm restrictions/opposition/status codes.
- [ ] Confirm limits and amount/currency precision.

## Posting
- [ ] Confirm `POST /api/v1/payment-postings`.
- [ ] Confirm atomic debit + CUT credit.
- [ ] Confirm `Idempotency-Key` semantics.
- [ ] Confirm same-key replay/conflict behavior.
- [ ] Confirm success/rejection/partial/unknown outcomes.
- [ ] Confirm bank posting reference.

## Posting lookup
- [ ] Confirm lookup by idempotency key.
- [ ] Confirm lookup by bank posting reference.
- [ ] Identify authoritative lookup.
- [ ] Confirm not-found/eventual-consistency semantics and retention.

## Fund reservation — OPTIONAL
- [ ] Confirm reservation support, expiry, lookup, capture and release.

## Reversal — OPTIONAL
- [ ] Confirm reversal support, time window, full/partial semantics, reason codes, authorization, idempotency and lookup.

## TFJ / EOD
- [ ] Confirm actual mechanism: callback, query, file/batch or other.
- [ ] Confirm cutoff/business date and matching keys.
- [ ] Confirm final status codes.
- [ ] Confirm callback security/replay behavior if applicable.
- [ ] Confirm fallback query and retention if applicable.
- [ ] Confirm duplicate/ambiguous/conflicting result handling.

## Approval evidence
For each approved capability record the bank/provider document reference,
version/date, approver or meeting reference, sandbox URL, security/certificate
requirements, differences from the provisional SIXPAY contract and final commit SHA.
'''
new2 = ROOT / "documentation/contracts/amplitude/BANK_CONFIRMATION_CHECKLIST.md"
if not new2.exists():
    write(str(new2.relative_to(ROOT)), checklist)
    print("[ok]   created BANK_CONFIRMATION_CHECKLIST.md")
else:
    print("[skip] BANK_CONFIRMATION_CHECKLIST.md already exists")

print("\nCB-1 changes applied. Review with:")
print("git diff -- documentation/contracts documentation/architecture/integration")
