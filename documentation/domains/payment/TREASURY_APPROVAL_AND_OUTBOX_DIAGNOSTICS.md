# Treasury deployment approval and Outbox diagnostic persistence

## Treasury configuration

The Payment policy never approves a financial institution or Treasury
configuration version implicitly. Missing approval map means no institution may
complete Treasury resolution, even if account lookup returns `SUCCESS`.

Each authorized deployment supplies an explicit overlay of the approved bank
codes and versions. For example, for a **locally authorized** Regional deployment:

```yaml
sixpay:
  payment:
    treasury:
      approval:
        approved-versions-by-institution:
          "[REGIONAL]": ["v1"]
```

This example does not authorize `REGIONAL` in other deployments. To deploy to
another institution, change the overlay, not Java. Spring Boot's bracket syntax
preserves the exact uppercase Map key during configuration binding. The base
`application-payment-banking-capabilities.yml` deliberately uses an empty map.

The existing `sixpay.payment.treasury.accounts` mapping remains the separately
configured bank/claim lookup. Its presence does not bypass the approval check.
Do not commit real account numbers or secrets in a deployment overlay.

## Outbox failure reason

The generic, provider-neutral `OutboxRelay` now records both exception simple
name and a *safe* message in `payment_outbox_events.failure_reason` when the
message consists only of a small set of non-sensitive characters. E.g.:
`PaymentDomainException: Conflicting Treasury-resolution evidence`.
The underlying Payment outbox persistence path and 1000-character column are
unchanged. Numeric values, account references, emails, URLs, embedded newlines
and otherwise untrusted diagnostics are not persisted as free text; these
fall back to the exception class name. A safe nested cause can be selected.

Do not include customer identifiers, credentials or bank payloads in domain
exception messages. The reason field is operational diagnostic data, not a
security-cleared audit record. Relay retries, exhaustion and DEAD semantics
remain unchanged; never manually replay a DEAD financial event without review.
