# Payment TFJ Reconciliation Runbook

## Core separation

```text
Posting != Reconciliation
```

A successful bank posting moves the Payment to pending TFJ confirmation. It
does not establish Treasury finality.

## Investigation

1. Confirm the posting outcome and bank posting reference.
2. Locate the authoritative TFJ entry.
3. Require one unique match proof.
4. Compare amount, currency, financial institution and posting identity.
5. Apply the TFJ evidence through `PaymentReconciliationUseCase`.
6. Observe the resulting domain state:
   - `TREASURY_INTEGRATED`;
   - `REVERSAL_REQUIRED`;
   - still pending reconciliation.

## Prohibited actions

- marking Treasury integrated without TFJ evidence;
- re-running posting to repair a TFJ mismatch;
- combining the posting and TFJ transactions;
- bypassing the Payment Aggregate Root.
