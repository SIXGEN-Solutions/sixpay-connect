# Lot 0 normalization — Payment source baseline

> Apply this section to `PAYMENT_SOURCE_BASELINE.md`.
>
> It supersedes only the obsolete identification metadata and adds the IA-1
> authority bridge. The existing detailed `PAY-SRC-*` catalogue remains in
> force unless explicitly contradicted below.

## IA-1 identification

| Property | Value |
| --- | --- |
| Gate | `IA-1 — PAYMENT DOMAIN BRIEF` |
| Lot | `0 — Baseline and Gate Scope` |
| Branch | `feat/payment-contract-pack` |
| Frozen source commit | `__PAYMENT_CONTRACT_PACK_HEAD_SHA__` |
| Baseline date | `2026-07-31` |
| Status | `BASELINE_PENDING_VALIDATION` |
| Code generation | **FORBIDDEN** |
| Primary Lot 0 authority | `documentation/ai/payment/PAYMENT_IA1_BASELINE.md` |

## Normalization rules

1. Any historic branch value such as `feat/customer-foundation-contract` is
   metadata from a previous baseline and is not the IA-1 authoritative branch.
2. IA-1 uses `feat/payment-contract-pack` at the frozen commit above.
3. The existing `PAY-SRC-*` and `SRC-*` identifiers remain stable.
4. Contract lifecycle and generation eligibility are determined by
   `CONTRACT_REGISTRY.yaml`, not by file location.
5. Any new IA-1 model rule must cite an existing source identifier,
   `PAY-BASE-*`, or an `OPEN-*` decision from `PAYMENT_IA1_BASELINE.md`.
6. Existing domain-model and state-machine artefacts are IA-1 inputs; they are
   not automatically final where Lot 0 records an open decision.
7. No implementation file can serve as the sole source of a Payment business
   rule.
