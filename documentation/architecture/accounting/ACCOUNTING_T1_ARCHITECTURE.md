# ACCOUNTING_T1 architecture

## Boundary flow

```text
Payment T0 COMPLETED
  -> Payment status POSTED_PENDING_TFJ
  -> finalized Payment financial snapshot
  -> durable internal Payment→Accounting event
  -> Accounting-owned local candidate projection
  -> local cutoff / eligibility / batch selection
  -> future T1 provider boundary (TO_DEFINE)
```

## Rules

- T0 remains closed. ACCOUNTING_T1 must not redesign the atomic T0 execution path.
- `payment` is the source of truth for T0 financial facts.
- `accounting` never reads Payment infrastructure, JPA entities or private repositories.
- Cross-module delivery is event push. Batch constitution is local projection pull.
- Accounting never reconstructs financial entries from mutable Payment state.
- Frozen `PaymentFinancialEntrySnapshot` facts are copied into an Accounting-owned projection in T1.2.
- Provider-compatible bkmvti payloads, if approved later, are mapping outputs and not SIXPAY domain persistence entities.
- Existing `accountingapi` code is implementation skeleton only until T1.4 approves the physical provider contract.

## Responsibility split

Payment:
- T0 outcome and evidence;
- financial snapshots;
- publication of the internal T1 input fact;
- final Payment state.

Accounting:
- candidate projection;
- cutoff and eligibility;
- batches;
- provider submission/recovery after contract approval;
- accounting reconciliation facts.

Bootstrap:
- composition only; no business logic.

Integration:
- provider-neutral technical support only.
