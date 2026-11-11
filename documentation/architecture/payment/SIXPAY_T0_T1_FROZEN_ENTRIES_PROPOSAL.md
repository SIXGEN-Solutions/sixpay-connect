# SIXPAY — T0/T1 frozen entries (local patch scope)

Source revision: `7ef86e602794205a137ede94ad1cd214b1ea9a0a`.

## Changes applied by local script

- Build and persist the existing reduced, immutable `PaymentFinancialEventSnapshot`
  and exactly two `PaymentFinancialEntrySnapshot` rows **only after** the local
  T0 posting context has been resolved. Persist them in the *same local DB
  transaction* as durable posting authorization. The bank POST remains outside
  that transaction. An authorization race rolls back the new snapshot.
- Existing `payment_financial_event_snapshots` ->
  `payment_financial_entry_snapshots` foreign-key ownership is retained.
- T1 selection can include *older*, unbatched, partner-verified candidates;
  the existing `accounting` projection `batchId` is the durable selection marker.
  The existing conditional `assignBatchIfUnassignedOrSame` remains authoritative.
- Add `AmplitudeBkmvti` as a bank-reference field inventory: all 54 legacy
  BKMVTI data fields plus technical `id`. This is not a JPA entity or an
  additional T0 HTTP payload.

## Explicitly pending human approval / bank contract publication

1. **Full BKMVTI values in durable Payment snapshots:** requires a reviewed
   persistence-schema migration and mapping for non-current fields, and
   corresponding Payment -> Accounting event/projection enrichment. Until
   approved, T1 reuses the existing reduced frozen financial entries only.
2. **Bank DCO refresh at T1:** no new endpoint or payload is invented. Define
   and approve a Core Banking lookup response and its permission/ownership;
   then add an Accounting-owned gateway. Preserve the original bank DCO as
   evidence; do not silently overwrite the immutable snapshot. A separately
   versioned derived accounting date is one possible future design.
3. **Legacy BKMVTI default values:** do not carry over hard-coded dates,
   payment fees or account defaults from historical code without bank approval.
4. **Cutoff semantics:** broadening T1 candidate search changes business
   selection. Verify with Business and Finance that `accountingBusinessDate`
   remains a valid filter after eventual bank DCO refresh.
5. **Concurrency and crash recovery:** run integration tests that verify a
   single local transaction manager governs the posting reservation, snapshot
   insert and `PaymentAtomicPersistencePort` update. Test crash before/after
   HTTP POST and unknown-provider-result recovery. Do not blindly replay POST.

The new `AmplitudeBkmvti` field inventory does **not** mean full BKMVTI persistence
or accounting API support has been completed. Both require explicit decisions.
