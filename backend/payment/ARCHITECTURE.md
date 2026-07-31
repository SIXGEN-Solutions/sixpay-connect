# Architecture Decision Record — Payment Value Objects

## Decision

Lot 3.2 implements the immutable identity and base Value Object layer of the
validated IA-1 Payment domain.

The Aggregate Root, snapshots, instruction identities, policies and events
remain outside this increment.

## Package decision

All base types reside under:

```text
com.sixpay.payment.domain.model
```

This keeps the domain API cohesive while avoiding premature technical
subpackage boundaries.

## Structural versus contextual validation

Value Objects enforce:

- non-nullity;
- lexical format;
- normalization;
- bounded collection size;
- positive allocation amounts;
- same-currency allocation composition;
- exact allocation total;
- failure category/disposition compatibility;
- protected-reference representation.

They do not validate:

- registry existence;
- bank activation;
- external uniqueness;
- authoritative evidence freshness;
- lifecycle transition eligibility;
- external-system outcomes.

Those remain aggregate invariants or policies.

## Platform reuse

Payment reuses:

- `CorrelationId` from `common`;
- `Money` and `ValueObject` from `shared-kernel`.

`PaymentRequestIdentity` adds the Payment-specific requirement that the reused
CorrelationId contain a canonical non-nil UUID.

No platform type is modified.

## Protected references

`DebtorAccountReference` and `TreasuryAccountReference` are final classes with
explicit equality and `toString()`.

- protected tokens never appear in `toString()`;
- masked display is excluded from identity equality;
- Treasury identity is bank + configuration ID + version;
- clear account values are never represented.

## Treasury allocation

`TreasuryAllocationIntent`:

- accepts 1 to 20 allocations;
- rejects duplicate beneficiaries;
- enforces positive same-currency amounts;
- verifies the exact total;
- stores a defensive immutable copy;
- stores allocations in canonical beneficiary-reference order.

## Failure classification

`PaymentFailure` enforces the validated category/disposition matrix and keeps
one bounded safe message.

## Controlled authorization

```text
scope: PAYMENT_DOMAIN_ONLY
currentIncrement: LOT_3_2_IDENTIFIERS_VALUE_OBJECTS
currentIncrementCodeGenerationAllowed: true
globalCodeGenerationAllowed: false
```

Every later increment still requires explicit activation.
