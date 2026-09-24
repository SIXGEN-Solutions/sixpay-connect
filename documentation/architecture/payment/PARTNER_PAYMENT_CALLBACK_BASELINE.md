# Partner Payment Callback Baseline

## Status

Canonical current-state architecture for outbound Partner Payment callbacks.

## Active contract

`partner-payment-callback-webhook-v1` is the active approved callback contract.

The callback is Partner-generic. No concrete Partner name participates in the
wire vocabulary or runtime callback payload.

## Ownership

- Payment owns callback business facts and eligibility.
- Notification owns delivery semantics according to the contract registry.
- Bootstrap owns runtime composition only.

## T0 guarantee

`CUT_CREDITED` is emitted only from durable completed T0 evidence and carries
the canonical bank posting reference. It does not establish TFJ finality.

## T1 guarantee

`TREASURY_INTEGRATED` is emitted only after successful TFJ/Treasury evidence is
durably persisted, uniquely matched and the durable Payment state is
`TREASURY_INTEGRATED`.

## Delivery

Delivery remains at-least-once. `eventId` is stable across retries;
`X-Webhook-Delivery-ID` changes per transport attempt; callback retries never
replay a financial command and delivery failure never changes Payment state.

## Signature

Callbacks use the active contract HMAC-SHA256 signing rule over timestamp,
HTTP method, request target and SHA-256 of the raw body.

## Runtime configuration

Outbound callback runtime configuration uses `sixpay.payment.callback.*`.
Partner inbound request/security configuration remains under
`sixpay.payment.partner.*`.
