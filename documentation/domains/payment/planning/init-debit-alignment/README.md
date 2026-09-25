# SIXPAY CONNECT — Init Debit Alignment

## Purpose

Versioned planning and decision index for the `init-debit-alignment` workstream.

This directory is a planning/decision aid. It does not override:
- authoritative implementation;
- architecture;
- requirements;
- physical contracts;
- `documentation/contracts/CONTRACT_REGISTRY.yaml`.

## Reference revision

- Repository: `SIXGEN-Solutions/sixpay-connect`
- Branch: `feat/repository-baseline-consolidation-cucumber`
- Reference SHA: `3857f80711e5e7d5c36abd627d00f6aa9e7d79e1`

## Payload baseline

Physical payload names are preserved.

| JSON field | Target |
|---|---|
| `LoginName` | REQUIRED; compatibility metadata, not authentication |
| `AppID` | REQUIRED; semantic `partnerIdentifier` |
| `endToEndId` | REQUIRED; external payment reference |
| `montantTotal` | REQUIRED |
| `devise` | OPTIONAL |
| `ribDebiteur` | OPTIONAL |
| `nomDebiteur` | OPTIONAL |
| `typeCreance` | REQUIRED |
| `NUI` | REQUIRED; NIU customer pivot |
| `dateExecution` | REQUIRED |
| `beneficiaires` | OPTIONAL |
| `callbackURL` | REQUIRED |

No new JSON field `partnerIdentifier` is introduced.
No `RIP` or `accountNumber` field is introduced by this workstream.

## Lot index

| Lot | Status | Artifact | Purpose |
|---|---|---|---|
| INIT-0 | COMPLETED-PROPOSAL | `INIT_0_DECISION_CLOSURE_IMPACT_AUDIT.md` | Close decisions and produce exact impact audit |
| INIT-1 | COMPLETED-PROPOSAL | `INIT_1_PARTNER_CUSTOMER_IDENTIFICATION.md` | Reconcile current physical contract with INIT-0 decisions and prepare the future amendment |
| INIT-2 | COMPLETED | `INIT_2_PARTNER_IDENTITY_ALIGNMENT.md` + INIT-2A/2B artifacts | Partner business and machine identity alignment implemented |
| INIT-3 | TECHNICALLY-CLOSED | `INIT_3_NIU_CORE_BANKING_RESOLUTION.md` | NIU-first Core Banking resolution |
| INIT-4 | AMENDMENT-PREPARED | `INIT_4_PAYMENT_CONTRACT_AMENDMENT_SYNCHRONIZATION_GATE.md` | Payment Contract Amendment & Synchronization Gate |
| INIT-5 | NOT STARTED | future implementation artifact | Payment input implementation after INIT-4 human contract validation |
| INIT-6 | NOT STARTED | future implementation artifact | Idempotency / persistence / recovery alignment |

## Mandatory usage for following lots

Before starting a subsequent lot:
1. load this `README.md`;
2. load every prior lot artifact marked as completed;
3. reload repository governance and current authoritative contracts;
4. compare prior decisions with the selected Git revision;
5. report any divergence instead of silently redefining a prior decision.

## Contract governance note

At reference SHA `2c7a575674fecaae044a23ac0663de7ea9763387`,
`tresorpay-payment-request-api-v1` is observed as:

- `lifecycleStatus: ACTIVE_MVP`
- `approvalStatus: APPROVED`
- `generationPolicy: ACTIVE`
- `codeGenerationAllowed: true`

This differs from a task assumption of
`PENDING_APPROVAL / REFERENCE_ONLY / codeGenerationAllowed=false`.

No physical contract is changed and no code generation is performed by INIT-0.
Any future public-contract change still requires the applicable human approval.


## INIT-2A — Partner business identity

See `INIT_2A_PARTNER_BUSINESS_IDENTITY.md`.

Partner owns the canonical mandatory unique `partnerIdentifier`, distinct from
its internal UUID. Machine identity binding remains outside INIT-2A.


## INIT-2B — Partner machine identity

See `INIT_2B_PARTNER_MACHINE_IDENTITY.md`.

M2M Partner callers use a Security-owned machine identity surface distinct from
human SIXPAY users. Security links the trusted machine subject to the canonical
Partner `partnerIdentifier`; Bootstrap resolves Partner through its public
surface and Payment owns the AppID consistency check.

The physical public AppID contract remains pending explicit approval.
