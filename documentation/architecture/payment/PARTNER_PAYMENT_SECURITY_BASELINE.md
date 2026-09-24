# Partner Payment Security Baseline

## Status

Canonical current-state architecture for the external Partner Payment security boundary.

## Active contract

The inbound Payment security contract is `partner-payment-request-api-v1`.

For the active MVP operation:

- Bearer JWT authentication is mandatory;
- `X-Subscription-Key` is mandatory;
- credentials are transport-only and are never persisted in Payment, events,
  audit payloads or logs;
- anti-replay and rate limiting are applied per Partner;
- OAuth2 client-credentials and mutual TLS are capability hooks only and are
  not activated as alternative inbound mechanisms unless an approved contract
  explicitly activates them.

## Partner-scoped capabilities

The implementation provides provider-neutral hooks for JWT validation,
Subscription Key, optional mTLS, optional API key, nonce/timestamp anti-replay,
rate limiting and structured audit.

Implemented capability does not imply contractual activation.

## Configuration ownership

- `sixpay.security.partner.*` -> Security
- `sixpay.payment.partner.*` -> Payment
- Bootstrap composes values and profiles only.

Secrets must come from environment or secret-management facilities.

## D2

Provider-named security runtime façades are replaced directly by Partner-generic
equivalents. No active TresorPay compatibility security boundary remains.

## D4

Partner Extensions are not an authentication mechanism. Secrets, keys, tokens
and credential material must never transit through dynamic Partner extensions.
Authentication/authorization configuration therefore remains separate from the
Partner Extension model.

## Boundaries

Security owns generic authentication primitives and security-filter composition.
Payment owns Payment-specific inbound request security semantics.
Bootstrap performs composition only.
