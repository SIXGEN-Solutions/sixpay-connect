# ACCOUNTING_T1 architecture

## Boundary flow

```text
Payment T0 COMPLETED
  -> Payment status POSTED_PENDING_TFJ
  -> finalized Payment financial snapshot
  -> durable internal Payment→Accounting event
  -> Accounting-owned local candidate projection
  -> local cutoff / eligibility / batch selection
  -> approved Core Banking Accounting submission
  -> authoritative submission recovery/reconciliation
  -> Amplitude TFJ confirmation
  -> Payment finality event
```

## Rules

- T0 remains closed. ACCOUNTING_T1 must not redesign the atomic T0 execution path.
- `payment` is the source of truth for T0 financial facts.
- `accounting` never reads Payment infrastructure, JPA entities or private repositories.
- Cross-module delivery is event push. Batch constitution is local projection pull.
- Accounting never reconstructs financial entries from mutable Payment state.
- Frozen `PaymentFinancialEntrySnapshot` facts are copied into an Accounting-owned projection in T1.2.
- Provider-compatible bkmvti payloads, if approved later, are mapping outputs and not SIXPAY domain persistence entities.
- `amplitude-accounting-entries-api-v1` is the approved provider contract for T1 submission/recovery.
- `amplitude-end-of-day-confirmation-api-v1` remains the bank-owned TFJ confirmation boundary.
- Manual operator execution launches SIXPAY T1; it never launches or emulates the bank-owned TFJ.

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
