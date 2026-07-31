# CONTRACT_REGISTRY.yaml — Lot 0 patch instructions

This file is intentionally provided as a patch fragment rather than a complete
replacement because the registry contains contract definitions that Lot 0 must
not accidentally rewrite.

Apply the following changes manually.

## 1. Normalize registry metadata

Replace:

```yaml
registry:
  id: "sixpay-contract-registry"
  gate: "IA-0R"
  step: "0.2"
  branch: "feat/customer-foundation-contract"
  classificationDate: "2026-07-30"
```

with:

```yaml
registry:
  id: "sixpay-contract-registry"
  gate: "IA-1_PAYMENT_DOMAIN_BRIEF"
  step: "LOT_0_BASELINE_AND_GATE_SCOPE"
  branch: "feat/payment-contract-pack"
  sourceCommit: "__PAYMENT_CONTRACT_PACK_HEAD_SHA__"
  classificationDate: "2026-07-31"
```

This is metadata normalization only. Do not change contract approval,
lifecycle or generation policies.

## 2. Normalize missing internal query contracts

In `missingMvpContracts.contracts`, preserve
`amplitude-payment-verification-api-v1` and normalize/add these internal
contracts according to the repository's authoritative paths:

```yaml
    - id: "payment-query-api-v1"
      path: "documentation/contracts/internal/payment-query-api-v1.yaml"
      capability: "PAYMENT_QUERY"
      direction: "AUTHORIZED_SIXPAY_CONSUMER_TO_SIXPAY"
      status: "TO_BE_PRODUCED_OR_APPROVED"

    - id: "observed-customer-query-api-v1"
      path: "documentation/contracts/internal/observed-customer-query-api-v1.yaml"
      capability: "OBSERVED_CUSTOMER_QUERY"
      direction: "AUTHORIZED_SIXPAY_CONSUMER_TO_SIXPAY"
      status: "TO_BE_PRODUCED_OR_APPROVED"

    - id: "payment-audit-query-api-v1"
      path: "documentation/contracts/internal/payment-audit-query-api-v1.yaml"
      capability: "PAYMENT_AUDIT_QUERY"
      direction: "AUTHORIZED_SIXPAY_CONSUMER_TO_SIXPAY"
      status: "TO_BE_PRODUCED_OR_APPROVED"
```

If these files already exist and are approved in the repository at patch time,
change `status` to the actual approved registry classification instead of
`TO_BE_PRODUCED_OR_APPROVED`.

Do not retain duplicate legacy IDs such as `sixpay-payment-query-api-v1` when
the approved file name is `payment-query-api-v1.yaml`.

## 3. Append Lot 0 rules

Append to `rules`:

```yaml
  - "IA-1 Payment modelling must use documentation/ai/payment/PAYMENT_IA1_BASELINE.md as the Lot 0 classification and decision authority."
  - "Every IA-1 Payment model rule must cite PAY-BASE-*, PAY-CONTRACT-*, PAY-AI-*, PAY-SRC-*, SRC-* or OPEN-*."
  - "A missing or pending-approval contract may preserve a boundary but cannot authorize code generation or invented payload fields."
```

## 4. Validation

After patching:

```bash
grep -R "__PAYMENT_CONTRACT_PACK_HEAD_SHA__" documentation/ai/payment documentation/contracts/CONTRACT_REGISTRY.yaml
```

The command must return no result before commit.
