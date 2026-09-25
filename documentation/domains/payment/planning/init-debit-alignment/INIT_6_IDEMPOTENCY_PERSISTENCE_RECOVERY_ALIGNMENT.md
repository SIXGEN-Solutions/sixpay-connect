# INIT-6 — Idempotency / Persistence / Recovery Alignment

## Status

IMPLEMENTED_LOCALLY_PENDING_VALIDATION

Baseline: `9f62dc5c8f43d4a49516272e516b49faceb9ce57`

## Approved structural decision

The human owner explicitly approved the INIT-6 structural evolution after the
audit identified that initiation idempotency and external Payment identity
were still source/global scoped.

## Implemented alignment

- `AppID` / `partnerIdentifier` is the canonical Partner business identity.
- `LoginName` and authenticated machine subject are excluded from the
  InitiateDebit business fingerprint.
- Initiation idempotency is scoped by
  `(partnerIdentifier, operation, idempotencyKey)`.
- Confirmation/OTP idempotency remains unchanged and unscoped by Partner in
  this lot.
- Payment external identity is scoped by
  `(partnerIdentifier, externalPaymentReference)`.
- Two Partners may therefore legitimately reuse the same external reference.
- Same Partner + same external reference + same canonical fingerprint replays
  the completed initiation acknowledgement.
- Same Partner + same external reference + different fingerprint returns the
  Payment-reference conflict path.
- `GET Payment` remains read-only and is not changed into a financial replay
  mechanism.
- No callback behavior is changed because INIT-6 found no need to alter the
  callback lifecycle.
- Pre-resolution Payment persistence remains NIU-first: bank institution and
  debtor account may still be absent until authoritative banking verification.

## Persistence

`payments.partner_identifier` is an indexed relational projection derived from
the Payment initiation context.

`payment_idempotency.partner_identifier` scopes Partner initiation records.
Existing non-initiation Payment idempotency records remain represented with a
NULL Partner scope and retain their own `(operation, idempotency_key)`
uniqueness through a partial unique index.

Because the project is still in development and the database is explicitly
recreatable, the approved change is applied to the canonical V300 baseline
rather than introducing a compatibility migration for deployed data.

## Validation required

Targeted first:

```bash
cd backend
mvn -pl payment,bootstrap -am test
```

Then:

```bash
mvn verify
```

Repository gates remain to be executed by the human operator according to
`TESTS_AND_GATES.md`.

No validation result is claimed by this document until those commands actually
finish with exit code 0.
