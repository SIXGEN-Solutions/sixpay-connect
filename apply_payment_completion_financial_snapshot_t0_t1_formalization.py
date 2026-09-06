#!/usr/bin/env python3
from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path.cwd()
BASE_COMMIT = "c36b246876b37723960f85df77fc335f2d1597c6"
MANIFEST_BASELINE = "09642ffaad0b1b0b6d2c16968866fb28f0162ab0"

FILES = {
    "financial_baseline": ROOT / "documentation/architecture/integration/payment-financial-execution-and-accounting-eod-baseline.md",
    "cb_baseline": ROOT / "documentation/architecture/integration/core-banking-api-baseline.md",
    "registry": ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml",
    "t0_contract": ROOT / "documentation/contracts/amplitude/amplitude-payment-posting-api-v1.yaml",
    "eod_contract": ROOT / "documentation/contracts/amplitude/amplitude-end-of-day-confirmation-api-v1.yaml",
    "landscape": ROOT / "documentation/architecture/integration/integration-landscape.md",
    "matrix": ROOT / "documentation/architecture/integration/integration-responsibility-matrix.md",
    "client_consolidation": ROOT / "documentation/architecture/integration/core-banking-client-consolidation.md",
    "checklist": ROOT / "documentation/contracts/amplitude/BANK_CONFIRMATION_CHECKLIST.md",
    "sync_flows": ROOT / "documentation/architecture/integration/synchronous-integration-flows.md",
    "accounting_batch": ROOT / "documentation/architecture/integration/payment-accounting-batch.md",
}

def require(ok: bool, message: str) -> None:
    if not ok:
        raise RuntimeError(message)

def git(*args: str, check: bool = True):
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=check,
    )

def read(path: Path) -> str:
    require(path.is_file(), f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    require(count == 1, f"{label}: expected exactly one occurrence, found {count}")
    return text.replace(old, new, 1)

def regex_once(text: str, pattern: str, repl: str, label: str, flags: int = 0) -> str:
    result, count = re.subn(pattern, repl, text, count=1, flags=flags)
    require(count == 1, f"{label}: expected exactly one match, found {count}")
    return result

require((ROOT / ".git").exists(), "Run from the SIXPAY repository root.")
head = git("rev-parse", "HEAD").stdout.strip()
branch = git("branch", "--show-current").stdout.strip() or "(detached HEAD)"

require(
    git("merge-base", "--is-ancestor", BASE_COMMIT, "HEAD", check=False).returncode == 0,
    f"HEAD {head} is not a descendant of inspected base {BASE_COMMIT}",
)
require(
    git("merge-base", "--is-ancestor", MANIFEST_BASELINE, "HEAD", check=False).returncode == 0,
    f"Manifest baseline {MANIFEST_BASELINE} is not an ancestor of HEAD {head}",
)

print(f"Branch: {branch}")
print(f"HEAD:   {head}")
print("Dirty worktree is allowed.")
print("No branch/commit/push/PR operation is performed.")

for p in FILES.values():
    require(p.is_file(), f"Missing required file: {p}")

planned: dict[Path, str] = {}

# Canonical financial baseline
text = read(FILES["financial_baseline"])
text = regex_once(
    text,
    r"### T0 — synchronous financial execution\n\n.*?### T\+1 — Accounting end-of-day treatment\n",
    """### T0 — synchronous financial execution

T0 is one protected synchronous Core Banking financial command.

Before provider submission, Payment freezes reduced immutable financial-event
and financial-entry snapshots. These are Payment-owned historical execution
facts; they are not the Payment aggregate and they are not copies of the full
historical Amplitude physical schema.

The Payment-owned Amplitude anti-corruption layer maps those snapshots to the
provider event payload corresponding functionally to:

- `bkeve`: the Core Banking Payment event;
- `bkmvti[]`: the debit/credit accounting lines attached to that event.

Only the provider attributes required by the approved SIXPAY mapping are stored.
Provider-specific values that influence the emitted payload and may change over
time must be frozen in the snapshot before submission.

The Core Banking system remains authoritative for all execution-time banking
controls and for the atomic execution of the submitted financial event.

Mandatory controls:

1. `ACCOUNT_EXISTS`
2. `ACCOUNT_ACTIVE`
3. `DEBIT_ALLOWED`
4. `CURRENCY_SUPPORTED`
5. `AVAILABLE_FUNDS_SUFFICIENT`
6. `PER_TRANSACTION_LIMIT_NOT_EXCEEDED`
7. `DAILY_LIMIT_NOT_EXCEEDED`
8. `OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED`

There is no standalone read-only Funds Control call in the MVP between OTP
confirmation and debit/credit.

If any mandatory control fails, Core Banking returns a conclusive business
rejection and no successful financial execution is recorded.

If every mandatory control passes, Core Banking validates the submitted debtor
and protected Partner creditor references and atomically executes the submitted
debit/credit event.

A timeout or transport failure after submission does not prove failure.
Unknown financial outcomes are recovered by authoritative lookup and are never
blindly resubmitted.

The approved T0 physical resource is:

- `POST /api/v1/payment-events`

Both authoritative recovery mechanisms are retained:

- `GET /api/v1/payment-events/{paymentReference}`;
- `GET /api/v1/payment-events/idempotency/{idempotencyKey}`.

T0 application integration security is OAuth2 Client Credentials plus mTLS.
Bank-SI network controls remain defense in depth and do not replace application
authentication and authorization.

### T+1 — Accounting end-of-day treatment
""",
    "financial baseline T0",
    flags=re.DOTALL,
)
text = regex_once(
    text,
    r"### T\+1 — Accounting end-of-day treatment\n\n.*?## 2\. MVP delivery mode and deferred file mode",
    """### T+1 — Accounting end-of-day treatment

T+1 is independent from T0 financial execution.

Only payments whose T0 financial execution succeeded are candidates.

The immutable financial-entry snapshots created for T0 are the source of the
accounting lines later selected for T+1. Accounting must not reconstruct those
lines from mutable current Payment, Partner or provider-configuration state.

Accounting:

1. selects eligible successful Payments for the applicable cut-off/business date;
2. obtains or uses authoritative TRESOR PAY status evidence for each candidate;
3. retains only candidates whose TRESOR PAY status is compatible with accounting eligibility;
4. obtains the immutable T0 financial-entry snapshot facts through an approved Payment-to-Accounting boundary;
5. builds an Accounting batch containing the frozen accounting lines;
6. submits that batch to the Core Banking accounting capability;
7. tracks provider acknowledgement, rejection and unknown outcomes;
8. reconciles the final accounting result.

SIXPAY owns generation and durable freezing of the accounting-line instructions
associated with its Payment event. Core Banking remains authoritative for
validation, acceptance, effective accounting posting and final accounting result.

A T+1 accounting failure does not retroactively turn a successful T0 financial
execution into an unpaid Payment.

## 2. MVP delivery mode and deferred file mode""",
    "financial baseline T1",
    flags=re.DOTALL,
)
text = replace_once(
    text,
    "- Payment owns the T0 business lifecycle and its Core Banking financial-execution orchestration.\n- Amplitude/Core Banking is authoritative for account state, funds, limits,\n  financial entries and bank references.\n- Accounting owns candidate selection, TRESOR PAY status evidence used for\n  accounting eligibility, batch constitution, submission tracking and reconciliation.\n- Core Banking owns accounting-entry generation and posting.",
    "- Payment owns the T0 business lifecycle, immutable reduced financial-event/entry snapshots and provider-specific event mapping.\n- Amplitude/Core Banking is authoritative for account state, funds, limits, execution acceptance, effective debit/credit and banking references.\n- Accounting owns candidate selection, TRESOR PAY status evidence used for accounting eligibility, consumption of immutable Payment financial-entry facts, batch constitution, submission tracking and reconciliation.\n- Core Banking owns validation and effective accounting posting of the T1 lines submitted by SIXPAY.",
    "financial baseline ownership",
)
planned[FILES["financial_baseline"]] = text

# Core Banking API baseline
text = read(FILES["cb_baseline"])
text = replace_once(
    text,
    '| Atomic T0 financial execution: mandatory controls + Treasury resolution/use + customer debit + Treasury credit | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_APPROVAL` | `REQUIRED` |\n'
    '| T0 financial outcome lookup | `amplitude-payment-posting-api-v1.yaml` | Payment | `PENDING_APPROVAL` | `REQUIRED` |',
    '| T0 Payment event execution: SIXPAY-built immutable event/entry snapshot + mandatory Core Banking controls + atomic debit/credit | `amplitude-payment-posting-api-v1.yaml` | Payment | `APPROVED` | `REQUIRED` |\n'
    '| T0 financial outcome lookup by Payment reference and Idempotency-Key | `amplitude-payment-posting-api-v1.yaml` | Payment | `APPROVED` | `REQUIRED` |',
    "cb inventory T0",
)
text = regex_once(
    text,
    r"### T0 financial execution\n\n.*?### Reversal",
    """### T0 financial execution

Approved physical contract:

`POST /api/v1/payment-events`

Payment freezes a reduced immutable financial-event snapshot and immutable
financial-entry snapshots before submission. The Payment-owned Amplitude mapper
converts those snapshots into the provider payload corresponding to `bkeve` plus
`bkmvti[]`.

The full historical Amplitude entity/table model is not imported into the
SIXPAY domain or persisted as-is.

Core Banking evaluates all eight mandatory execution-time banking controls and
owns atomic execution of the submitted debit/credit event.

Application integration security is OAuth2 Client Credentials plus mTLS.
Network controls inside the bank SI remain defense in depth.

A financial command with an uncertain transport outcome is never blindly retried.

### T0 authoritative lookup

Both approved recovery mechanisms are retained:

- `GET /api/v1/payment-events/{paymentReference}`
- `GET /api/v1/payment-events/idempotency/{idempotencyKey}`

### Reversal""",
    "cb T0 signatures",
    flags=re.DOTALL,
)
text = regex_once(
    text,
    r"### Accounting T\+1 / TFJ / EOD\n\n.*?CSV/file submission is explicitly deferred until a separate approved file\ncontract exists\.",
    """### Accounting T+1 / TFJ / EOD

Accounting first constitutes a batch from financially successful Payments whose
TRESOR PAY status evidence satisfies the accounting-eligibility rule.

Payment exposes immutable financial-entry snapshot facts through an approved
internal boundary. Accounting never reads Payment JPA repositories or provider
entities directly.

For the MVP, Accounting submits a batch of frozen SIXPAY-generated accounting
lines through `AccountingBatchGateway` to the Core Banking Accounting API.
Core Banking validates and effectively posts/accounts those submitted lines and
returns authoritative results.

The final physical Accounting API endpoint/path and complete wire schema remain
`TO_DEFINE` until the dedicated T1 contract is formalized.

`amplitude-end-of-day-confirmation-api-v1.yaml` remains the result-confirmation /
reconciliation contract, not the batch-submission contract.

CSV/file submission is explicitly deferred until a separate approved file
contract exists.""",
    "cb T1",
    flags=re.DOTALL,
)
planned[FILES["cb_baseline"]] = text

# Registry
registry = read(FILES["registry"])
registry_replacement = """  - id: "amplitude-payment-posting-api-v1"
    path: "documentation/contracts/amplitude/amplitude-payment-posting-api-v1.yaml"
    domain: "integration"
    businessOwner: "payment"
    deliveryOwner: "integration"
    capability: "BANKING_PAYMENT_EVENT_EXECUTION"
    direction: "SIXPAY_TO_AMPLITUDE"
    sourceSystem: "SIXPAY"
    systemOfRecord: "AMPLITUDE"
    lifecycleStatus: "ACTIVE_MVP"
    approvalStatus: "APPROVED"
    generationPolicy: "ACTIVE"
    codeGenerationAllowed: true
    governanceDecision: "D00-CB-OWNERSHIP"
    gate: "PAYMENT_COMPLETION"
    phaseStep: "2.3.T0"
    security:
      status: "APPROVED_CONTRACT_PROFILE"
      authentication: ["OAUTH2_CLIENT_CREDENTIALS", "MUTUAL_TLS"]
      requiredScopes:
        execute: "amplitude.payment.execution"
        read: "amplitude.payment.execution.read"
      correlationHeader: "X-Correlation-ID"
      institutionHeader: "X-Financial-Institution-Code"
      idempotencyHeader: "Idempotency-Key"
      operationalParameters: "EXTERNAL_CONFIGURATION"
    mvpUsage:
      included: true
      purpose:
        - "Freeze Payment-owned immutable reduced financial-event and financial-entry snapshots before provider submission"
        - "Map those snapshots to the provider Payment event payload corresponding to bkeve plus bkmvti lines"
        - "Execute POST /api/v1/payment-events as the single protected T0 Core Banking financial command"
        - "Require all eight authoritative execution-time banking controls"
        - "Require Core Banking atomic execution of debit and credit"
        - "Recover an uncertain outcome by Payment reference"
        - "Recover an uncertain outcome by original Idempotency-Key"
      constraints:
        - "SIXPAY persists reduced immutable snapshots, not the complete historical Amplitude bkeve/bkmvti entity/table schema"
        - "Provider-specific payloads and mappings remain in Payment infrastructure"
        - "No standalone read-only Funds Control Core Banking call exists in the MVP"
        - "The canonical debtor account reference comes from the earlier VERIFIED Customer Verification result"
        - "The protected creditor account reference is resolved from Partner-owned data before snapshot finalization"
        - "A finalized/submitted financial snapshot is immutable"
        - "T1 consumes immutable financial-entry facts through an approved Payment-to-Accounting boundary"
        - "Accounting never reads Payment infrastructure/JPA directly"
        - "Same Idempotency-Key plus same logical request maps to the same financial operation/result"
        - "Same Idempotency-Key plus a different logical request is a conflict"
        - "Unknown financial outcomes permit authoritative lookup only, never blind replay"
        - "OAuth2 Client Credentials and mTLS remain application-level security even inside the same bank SI"
      approvedOperations:
        execute: "POST /api/v1/payment-events"
        lookupByPaymentReference: "GET /api/v1/payment-events/{paymentReference}"
        lookupByIdempotencyKey: "GET /api/v1/payment-events/idempotency/{idempotencyKey}"
    approvalPrerequisite: []
"""
registry = regex_once(
    registry,
    r'  - id: "amplitude-payment-posting-api-v1"\n.*?(?=\n  - id: "tresorpay-payment-status-webhook-v1")',
    registry_replacement,
    "registry T0",
    flags=re.DOTALL,
)
registry = replace_once(
    registry,
    '        - "Only a matched successful result establishes TREASURY_INTEGRATED"\n'
    '        - "An unmatched, ambiguous or conflicting result is quarantined"',
    '        - "T1 batch submission uses immutable SIXPAY-generated financial-entry snapshots; this contract governs result/reconciliation only"\n'
    '        - "Core Banking validates and effectively posts/accounts the submitted T1 lines"\n'
    '        - "Only a matched successful result establishes TREASURY_INTEGRATED"\n'
    '        - "An unmatched, ambiguous or conflicting result is quarantined"',
    "registry EOD",
)
planned[FILES["registry"]] = registry

# T0 contract
contract = """openapi: 3.1.0

info:
  title: SIXPAY CONNECT - Amplitude Payment Event API
  version: 1.0.0
  summary: Protected synchronous T0 execution of a SIXPAY-built Core Banking Payment event.
  description: |
    Approved T0 target contract.

    SIXPAY Payment freezes reduced immutable financial-event and financial-entry
    snapshots, then its Amplitude anti-corruption layer maps those snapshots to
    the provider event corresponding functionally to `bkeve` and `bkmvti[]`.

    SIXPAY does not persist the complete historical Amplitude entity/table
    schema. The exact provider field subset/code tables remain a mapping
    implementation input and must be derived from approved bank evidence before
    provider DTO generation.

    Core Banking remains authoritative for execution-time banking controls and
    atomic execution of the submitted event.

  x-sixpay-contract:
    registryId: "amplitude-payment-posting-api-v1"
    gate: "PAYMENT_COMPLETION"
    phaseStep: "2.3.T0"
    lifecycleStatus: "ACTIVE_MVP"
    approvalStatus: "APPROVED"
    generationPolicy: "ACTIVE"
    codeGenerationAllowed: true
    governanceDecision: "D00-CB-OWNERSHIP"
    domain: "integration"
    businessOwner: "payment"
    deliveryOwner: "integration"
    capability: "BANKING_PAYMENT_EVENT_EXECUTION"
    direction: "SIXPAY_TO_AMPLITUDE"
    sourceSystem: "SIXPAY"
    systemOfRecord: "AMPLITUDE"
    approvedContractDesign:
      execute: "POST /api/v1/payment-events"
      lookupByPaymentReference: "GET /api/v1/payment-events/{paymentReference}"
      lookupByIdempotencyKey: "GET /api/v1/payment-events/idempotency/{idempotencyKey}"
      applicationSecurity: ["OAUTH2_CLIENT_CREDENTIALS", "MUTUAL_TLS"]
      executeScope: "amplitude.payment.execution"
      readScope: "amplitude.payment.execution.read"
    snapshotPolicy:
      owner: "payment"
      persistence: "REDUCED_IMMUTABLE_SIXPAY_SNAPSHOTS"
      providerEntityPersistence: false
      finalizedSnapshotMutable: false
      t1Reuse: "ACCOUNTING_CONSUMES_FROZEN_ENTRY_FACTS_THROUGH_APPROVED_BOUNDARY"
    providerMapping:
      exactFieldSubsetStatus: "TO_BE_DERIVED_FROM_APPROVED_BANK_EVIDENCE"
      fullHistoricalSchemaImportAllowed: false
    limitations:
      - "NO_STANDALONE_READ_ONLY_FUNDS_CONTROL_CALL"
      - "NO_BLIND_FINANCIAL_REPLAY"
      - "NO_COMPLETE_AMPLITUDE_BKEVE_BKMVTI_SCHEMA_PERSISTENCE"
      - "NO_T1_BATCH_SUBMISSION_ENDPOINT_DEFINED_BY_THIS_CONTRACT"

servers:
  - url: https://amplitude-api.example
    description: Environment-specific placeholder.

security:
  - oauth2ClientCredentials:
      - amplitude.payment.execution.read
    mutualTLS: []

paths:
  /api/v1/payment-events:
    post:
      tags: [Payment events]
      operationId: registerPaymentEventToCoreBanking
      summary: Register and execute the SIXPAY Payment event
      security:
        - oauth2ClientCredentials:
            - amplitude.payment.execution
          mutualTLS: []
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - $ref: '#/components/parameters/FinancialInstitutionCode'
        - $ref: '#/components/parameters/IdempotencyKey'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PaymentEventEnvelope'
      responses:
        '200':
          description: Conclusive COMPLETED or REJECTED result.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentEventResult'
        '202':
          description: Accepted but outcome not yet conclusive.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentEventResult'
        '400': { $ref: '#/components/responses/ProblemResponse' }
        '401': { $ref: '#/components/responses/ProblemResponse' }
        '403': { $ref: '#/components/responses/ProblemResponse' }
        '409': { $ref: '#/components/responses/ProblemResponse' }
        '422': { $ref: '#/components/responses/ProblemResponse' }
        '503': { $ref: '#/components/responses/ProblemResponse' }

  /api/v1/payment-events/{paymentReference}:
    get:
      tags: [Payment event recovery]
      operationId: getPaymentEventByPaymentReference
      security:
        - oauth2ClientCredentials:
            - amplitude.payment.execution.read
          mutualTLS: []
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - $ref: '#/components/parameters/FinancialInstitutionCode'
        - $ref: '#/components/parameters/PaymentReferencePath'
      responses:
        '200':
          description: Authoritative T0 result.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentEventResult'
        '404': { $ref: '#/components/responses/ProblemResponse' }
        '503': { $ref: '#/components/responses/ProblemResponse' }

  /api/v1/payment-events/idempotency/{idempotencyKey}:
    get:
      tags: [Payment event recovery]
      operationId: getPaymentEventByIdempotencyKey
      security:
        - oauth2ClientCredentials:
            - amplitude.payment.execution.read
          mutualTLS: []
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - $ref: '#/components/parameters/FinancialInstitutionCode'
        - $ref: '#/components/parameters/IdempotencyKeyPath'
      responses:
        '200':
          description: Authoritative T0 result.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PaymentEventResult'
        '404': { $ref: '#/components/responses/ProblemResponse' }
        '503': { $ref: '#/components/responses/ProblemResponse' }

components:
  securitySchemes:
    oauth2ClientCredentials:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: https://identity.amplitude.example/oauth2/token
          scopes:
            amplitude.payment.execution: Execute a T0 Payment event
            amplitude.payment.execution.read: Read authoritative T0 results
    mutualTLS:
      type: mutualTLS

  parameters:
    CorrelationId:
      name: X-Correlation-ID
      in: header
      required: true
      schema: { type: string, format: uuid }
    FinancialInstitutionCode:
      name: X-Financial-Institution-Code
      in: header
      required: true
      schema: { type: string, minLength: 2, maxLength: 35 }
    IdempotencyKey:
      name: Idempotency-Key
      in: header
      required: true
      schema: { type: string, minLength: 16, maxLength: 128 }
    IdempotencyKeyPath:
      name: idempotencyKey
      in: path
      required: true
      schema: { type: string, minLength: 16, maxLength: 128 }
    PaymentReferencePath:
      name: paymentReference
      in: path
      required: true
      schema:
        $ref: '#/components/schemas/PaymentReference'

  schemas:
    PaymentReference:
      type: string
      minLength: 1
      maxLength: 100

    PaymentEventEnvelope:
      type: object
      additionalProperties: false
      required:
        - paymentReference
        - snapshotVersion
        - providerEvent
        - providerEntries
        - requestedAt
      properties:
        paymentReference:
          $ref: '#/components/schemas/PaymentReference'
        snapshotVersion:
          type: string
          minLength: 1
          maxLength: 32
        providerEvent:
          type: object
          description: |
            Provider-specific reduced `bkeve`-equivalent payload. The exact
            approved field subset is supplied by the Payment-owned Amplitude
            mapping and must not be inferred from this placeholder schema.
          additionalProperties: true
        providerEntries:
          type: array
          minItems: 1
          description: |
            Provider-specific reduced `bkmvti`-equivalent lines built from the
            immutable SIXPAY financial-entry snapshots. Exact fields/code tables
            require approved bank mapping evidence.
          items:
            type: object
            additionalProperties: true
        requestedAt:
          type: string
          format: date-time

    PaymentExecutionCheck:
      type: object
      additionalProperties: false
      required: [type, result]
      properties:
        type:
          type: string
          enum:
            - ACCOUNT_EXISTS
            - ACCOUNT_ACTIVE
            - DEBIT_ALLOWED
            - CURRENCY_SUPPORTED
            - AVAILABLE_FUNDS_SUFFICIENT
            - PER_TRANSACTION_LIMIT_NOT_EXCEEDED
            - DAILY_LIMIT_NOT_EXCEEDED
            - OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED
        result:
          type: string
          enum: [PASS, FAIL, UNKNOWN]
        reasonCode:
          type: [string, 'null']

    PaymentEventResult:
      type: object
      additionalProperties: false
      required:
        - paymentReference
        - outcome
        - checks
        - observedAt
      properties:
        paymentReference:
          $ref: '#/components/schemas/PaymentReference'
        outcome:
          type: string
          enum: [COMPLETED, REJECTED, UNKNOWN]
        checks:
          type: array
          minItems: 8
          maxItems: 8
          items:
            $ref: '#/components/schemas/PaymentExecutionCheck'
        bankReference:
          type: [string, 'null']
          maxLength: 128
        reasonCode:
          type: [string, 'null']
          maxLength: 64
        observedAt:
          type: string
          format: date-time

    Problem:
      type: object
      additionalProperties: true
      required: [type, title, status]
      properties:
        type: { type: string }
        title: { type: string }
        status: { type: integer }
        detail: { type: string }

  responses:
    ProblemResponse:
      description: Standardized technical or business error response.
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/Problem'
"""
planned[FILES["t0_contract"]] = contract

# EOD result/reconciliation contract
text = read(FILES["eod_contract"])
text = replace_once(
    text,
    """    This contract does not describe Accounting batch submission and SIXPAY does
    not generate the Core Banking accounting entries. The Core Banking
    accounting capability owns entry generation/posting. Callback versus lookup
    priority remains subject to the revised contract review.
""",
    """    This contract does not describe Accounting batch submission.

    SIXPAY owns generation and durable freezing of the reduced accounting lines
    derived from its T0 Payment financial-event snapshots. The future T1
    Accounting submission reuses those immutable lines. Core Banking owns
    validation, acceptance and effective accounting posting of the submitted
    lines.

    Callback versus lookup priority remains subject to the revised contract review.
""",
    "EOD ownership",
)
planned[FILES["eod_contract"]] = text

# Landscape
text = read(FILES["landscape"])
text = replace_once(
    text,
    '| INT-07 | Payment → Core Banking T0 financial execution | outbound | one protected synchronous financial command | adapter/domain foundation only | existing posting/funds/lookup foundations; physical transport still blocked | revised `amplitude-payment-posting-api-v1.yaml`, `PENDING_APPROVAL`, `REFERENCE_ONLY` |\n'
    '| INT-08 | Payment → Core Banking T0 outcome lookup | outbound | synchronous provider query | interface only | lookup by idempotency key and bank reference | same T0 contract; required for uncertain outcomes |',
    '| INT-07 | Payment → Core Banking T0 Payment event | outbound | one protected synchronous financial command | contract approved; implementation adaptation pending | Payment freezes reduced immutable event/entry snapshots then maps them to provider payload | `amplitude-payment-posting-api-v1.yaml`, `APPROVED`, `ACTIVE` |\n'
    '| INT-08 | Payment → Core Banking T0 outcome lookup | outbound | synchronous provider query | contract approved; implementation adaptation pending | retain lookup by Payment reference and original Idempotency-Key | same approved T0 contract |',
    "landscape T0",
)
text = replace_once(
    text,
    '| INT-11 | Payment → Accounting | internal Accounting-owned candidate source / scheduled T+1 constitution | planned but domain/application foundations exist | `PaymentAccountingCandidateSource`, `AccountingBatchConstitutionService`, `AccountingBatchGateway`; production candidate source not wired | internal Payment→Accounting contract `TO_DEFINE`; Core Banking Accounting API contract `TO_DEFINE`; CSV deferred |',
    '| INT-11 | Payment → Accounting | internal Accounting-owned candidate source / scheduled T+1 constitution | planned but domain/application foundations exist | `PaymentAccountingCandidateSource`, `AccountingBatchConstitutionService`, `AccountingBatchGateway`; production candidate source not wired | internal boundary must expose immutable T0 financial-entry snapshot facts; Core Banking Accounting API contract remains `TO_DEFINE`; CSV deferred |',
    "landscape T1",
)
text = replace_once(
    text,
    '- Accounting contract: Accounting owns consumption; Payment owns source facts; Core Banking owns external file/API acceptance.',
    '- Accounting contract: Accounting owns consumption/batching; Payment owns immutable T0 financial-event/entry source facts; Core Banking owns external API acceptance and effective accounting posting.',
    "landscape ownership",
)
planned[FILES["landscape"]] = text

# Responsibility matrix
text = read(FILES["matrix"])
text = replace_once(
    text,
    '| INT-07 | Payment | Core Banking T0 financial execution | one synchronous financial command with unknown-outcome handling | revised `amplitude-payment-posting-api-v1.yaml` (`REFERENCE_ONLY`) | Core Banking provider; Payment owns business orchestration/mapping | OAuth2/mTLS; mandatory idempotency and correlation | all 8 controls + protected Treasury resolution/use + debit + credit; never blind-retry unknown outcome | contract, concurrency, idempotency, unknown-outcome, sandbox, E2E | FOUNDATION / PENDING CONTRACT APPROVAL | 5.4 |\n'
    '| INT-08 | Payment | Core Banking T0 outcome lookup | provider query, synchronous | same T0 financial-execution contract | Core Banking provider; Payment owns orchestration | OAuth2/mTLS | not-found distinct from unavailable; authoritative lookup required after uncertain command | lookup fixtures and reconciliation E2E | FOUNDATION | 5.4 |',
    '| INT-07 | Payment | Core Banking T0 Payment event | one synchronous financial command with unknown-outcome handling | `amplitude-payment-posting-api-v1.yaml` (`APPROVED`, `ACTIVE`) | Payment owns immutable snapshot + provider mapping; Core Banking owns authoritative controls/execution | OAuth2 Client Credentials + mTLS; mandatory idempotency and correlation | freeze reduced bkeve/bkmvti-equivalent snapshots; all 8 controls; atomic execution; never blind-retry unknown outcome | snapshot/mapping, contract, concurrency, idempotency, unknown-outcome, sandbox, E2E | APPROVED CONTRACT / IMPLEMENTATION PENDING | 5.4 |\n'
    '| INT-08 | Payment | Core Banking T0 outcome lookup | provider query, synchronous | same approved T0 contract | Core Banking provider; Payment owns orchestration | OAuth2 Client Credentials + mTLS | retain both Payment-reference and Idempotency-Key lookup | dual lookup fixtures and reconciliation E2E | APPROVED CONTRACT / IMPLEMENTATION PENDING | 5.4 |',
    "matrix T0",
)
text = replace_once(
    text,
    '| INT-11 | Payment | Accounting | Accounting-owned candidate source + scheduled T+1 batch constitution; Core Banking API submission in MVP | Payment→Accounting contract `TO_DEFINE`; Core Banking Accounting API contract `TO_DEFINE`; CSV contract deferred | Accounting; Payment owns source facts; Core Banking owns accounting-entry generation/posting | API: OAuth2/mTLS profile to approve; CSV/SFTP security deferred | verify TRESOR PAY status before eligibility; reconcile rejected/unknown provider outcomes; no blind batch replay | candidate-source contract, API provider stub, reconciliation/idempotency, E2E; file tests only when CSV enabled | PLANNED / FOUNDATIONS PRESENT | 5.6 |',
    '| INT-11 | Payment | Accounting | Accounting-owned candidate source + scheduled T+1 batch constitution; Core Banking API submission in MVP | Payment→Accounting immutable financial-entry snapshot contract `TO_DEFINE`; Core Banking Accounting API contract `TO_DEFINE`; CSV contract deferred | Accounting owns batching; Payment owns immutable entry facts; Core Banking owns validation/effective accounting posting | API: OAuth2/mTLS profile to approve; CSV/SFTP security deferred | verify TRESOR PAY status before eligibility; use frozen T0 lines, never reconstruct from mutable state; reconcile rejected/unknown provider outcomes; no blind batch replay | candidate-source/projection contract, immutable snapshot tests, API provider stub, reconciliation/idempotency, E2E | PLANNED / FOUNDATIONS PRESENT | 5.6 |',
    "matrix T1",
)
planned[FILES["matrix"]] = text

# Client consolidation
text = read(FILES["client_consolidation"])
text = replace_once(
    text,
    '| Posting | Payment | `PostingGateway` | `AmplitudePostingClient` |\n| Posting lookup | Payment | `LookupGateway` | `AmplitudePostingStatusClient` |',
    '| Payment event execution | Payment | existing posting/execution boundary to align | `AmplitudePaymentEventClient` |\n| Payment event lookup | Payment | existing lookup boundary to align | `AmplitudePaymentEventStatusClient` |',
    "client matrix",
)
text = regex_once(
    text,
    r"## Bank-approved endpoints\n\n.*?## Safety",
    """## Bank-approved endpoints

Approved T0 operations:

- `POST /api/v1/payment-events`
- `GET /api/v1/payment-events/{paymentReference}`
- `GET /api/v1/payment-events/idempotency/{idempotencyKey}`

Payment owns the reduced immutable financial snapshot and the Amplitude-specific
mapping used to construct the provider event. `backend/integration` remains
provider-neutral.

The T1 Accounting submission endpoint remains `TO_DEFINE`.

## Safety""",
    "client endpoints",
    flags=re.DOTALL,
)
planned[FILES["client_consolidation"]] = text

# Checklist
text = read(FILES["checklist"])
text = regex_once(
    text,
    r"## Funds check\n.*?## Fund reservation — OPTIONAL",
    """## T0 Payment event
- [x] Confirm `POST /api/v1/payment-events`.
- [x] Confirm SIXPAY prepares the provider Payment event and its accounting lines.
- [x] Confirm SIXPAY persists reduced immutable snapshots, not the complete historical provider schema.
- [x] Confirm Core Banking owns authoritative execution-time controls and debit/credit atomicity.
- [x] Confirm both authoritative lookups: Payment reference and original Idempotency-Key.
- [x] Confirm OAuth2 Client Credentials + mTLS application security.
- [ ] Confirm the exact provider field subset/code tables required for the SIXPAY bkeve/bkmvti-equivalent payload.
- [ ] Confirm amount/currency precision and provider date/code semantics.

## T1 Accounting
- [x] Confirm SIXPAY reuses immutable T0 financial-entry snapshots for T1.
- [x] Confirm Core Banking validates and effectively posts/accounts submitted lines.
- [ ] Confirm physical Accounting API endpoint and final batch/line wire schema.
- [ ] Confirm cut-off/business-date and reconciliation semantics.

## Fund reservation — OPTIONAL""",
    "checklist",
    flags=re.DOTALL,
)
planned[FILES["checklist"]] = text

# Synchronous flows
text = read(FILES["sync_flows"])
text = regex_once(
    text,
    r"## 6\. Flow SYN-05 — Payment controls funds\n\n.*?## 9\. Flow SYN-08 — Payment reverses a posting",
    """## 6. Flow SYN-05 — Payment prepares and executes the T0 Payment event

```text
Payment
  -> finalize immutable financial-event snapshot
  -> finalize immutable financial-entry snapshots
  -> Payment-owned Amplitude mapper
  -> AmplitudePaymentEventClient
  -> POST /api/v1/payment-events
  -> Core Banking authoritative controls + atomic debit/credit
```

### Mandatory safety rules

- finalized/submitted financial snapshots are immutable;
- provider-specific event/entry DTOs remain in Payment infrastructure;
- SIXPAY persists reduced snapshots, not the full historical Amplitude schema;
- every financial command has a stable idempotency key;
- timeout after possible transmission creates `UNKNOWN`, not automatic failure;
- blind retry is forbidden;
- application security is OAuth2 Client Credentials plus mTLS.

## 7. Flow SYN-06 — Payment resolves T0 outcome

```text
Payment reconciliation
  -> Payment-event lookup adapter/client
  -> lookup by Payment reference OR original Idempotency-Key
  -> resolve success / rejection / still unknown
```

### Tests

- found by Payment reference;
- found by Idempotency-Key;
- not found;
- inconsistent provider response;
- temporary unavailability;
- repeated reconciliation is idempotent.

## 8. Flow SYN-08 — Payment reversal

Reversal remains outside the newly approved T0 Payment-event surface and
requires a separate explicit contract/enablement decision before implementation.

## 9. Flow SYN-08 — Payment reverses a posting""",
    "sync flows",
    flags=re.DOTALL,
)
planned[FILES["sync_flows"]] = text

# Accounting batch architecture
text = read(FILES["accounting_batch"])
text = replace_once(
    text,
    """Accounting does not depend on the Payment module and never receives the Payment
aggregate. Payment facts are converted by a future composition adapter into the
Accounting-owned `AccountingPaymentCandidate`.
""",
    """Accounting does not depend on Payment infrastructure and never receives the
Payment aggregate or Payment JPA entities/repositories.

Payment owns immutable T0 financial-event and financial-entry snapshot facts.
An approved internal boundary/composition adapter projects the subset required
by Accounting into Accounting-owned candidate/item models.

Accounting therefore consumes frozen historical execution facts; it does not
rebuild accounting lines from current Payment, Partner or provider configuration.
""",
    "accounting boundary",
)
text = replace_once(
    text,
    """## Explicitly outside Lot 5.6.1 / SIXPAY accounting-domain model

- TFJ physical format;
- accounting codes;
- debit/credit rules;
- TFJ control totals;
- file naming;
- SFTP host/key/directories;
- technical SFTP acknowledgement.

Those remain responsibilities of the downstream Accounting/TFJ provider.
""",
    """## Financial-entry source

The debit/credit line instructions used by T1 originate from immutable T0
financial-entry snapshots produced and frozen by Payment.

Accounting may enrich them only with Accounting-owned batch metadata and
eligibility/reconciliation evidence. It must not mutate the original financial
meaning of the frozen lines.

Core Banking validates and effectively posts/accounts the submitted lines. The
physical Accounting API endpoint and final provider batch/line schema remain
`TO_DEFINE`.

## Explicitly outside the current formalisation

- physical Accounting API endpoint;
- exact final provider field/code subset for the T1 batch;
- TFJ control totals;
- file naming;
- SFTP host/key/directories;
- technical SFTP acknowledgement.

These require the dedicated T1 implementation/contract lot.
""",
    "accounting source",
)
planned[FILES["accounting_batch"]] = text

require(len(planned) == len(FILES), f"Expected {len(FILES)} files, got {len(planned)}")

for fragment in [
    "POST /api/v1/payment-events",
    "GET /api/v1/payment-events/{paymentReference}",
    "GET /api/v1/payment-events/idempotency/{idempotencyKey}",
    'approvalStatus: "APPROVED"',
    'generationPolicy: "ACTIVE"',
    "codeGenerationAllowed: true",
    "REDUCED_IMMUTABLE_SIXPAY_SNAPSHOTS",
]:
    require(fragment in planned[FILES["t0_contract"]], f"T0 contract missing {fragment}")

require(
    "SIXPAY does not generate the Core Banking accounting entries"
    not in planned[FILES["eod_contract"]],
    "Old T1 ownership statement still present.",
)

print("\nPlanned updates:")
for path in planned:
    print(" -", path.relative_to(ROOT))

for path, content in planned.items():
    path.write_text(content, encoding="utf-8", newline="\n")

print("\nFORMALIZATION APPLIED")
print("No Java source, migration, branch, commit, push or PR operation was performed.")
print("\nRecommended validations:")
print("  py scripts/verify_master_prompt_input_manifest.py")
print("  py scripts/verify_documentation_contract_references.py")
print("  py scripts/verify_documentation_classification.py")
print("  py scripts/verify_ai_documentation.py")
print("  py scripts/verify_documentation_final.py")
print("  py scripts/verify_master_engineering_prompt.py")
print("  git diff --check")
print("  git diff --stat")
print("  git status --short")
