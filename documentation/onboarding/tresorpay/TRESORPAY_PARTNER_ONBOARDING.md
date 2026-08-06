# TresorPay Partner Onboarding

## 1. Contract baseline

TresorPay integrates with:

```text
POST /v1/payments/initiate
```

Authoritative contracts:

- `documentation/contracts/external/payment-command-api-v1.yaml`;
- `documentation/contracts/external/tresorpay-security-profile-v1.yaml`;
- `documentation/contracts/external/tresorpay-public-error-catalogue-v1.yaml`;
- `documentation/contracts/external/tresorpay-payment-callback-api-v1.yaml`.

## 2. Prerequisites requested from TresorPay

- OAuth2 client identifier;
- JWT issuer and token endpoint;
- confirmed audience and scope;
- client certificate subject and issuing CA;
- outbound source IP addresses;
- registered `LoginName`;
- callback hostname and certificate;
- callback JWS verification capability;
- technical and operational contacts.

## 3. Inbound security

Production baseline:

1. TLS 1.3;
2. mutual TLS;
3. OAuth2 client-credentials JWT;
4. audience `sixpay-payment-api`;
5. scope `payment.initiate`;
6. `LoginName` equals the authenticated partner identity;
7. API key disabled unless a formally approved compatibility requirement exists.

Mandatory headers:

```http
Authorization: Bearer <JWT>
Idempotency-Key: <partner-generated-key>
X-Correlation-ID: <UUID>
X-Request-Timestamp: <RFC3339 instant>
X-Request-Nonce: <unique value>
Content-Type: application/json
```

## 4. Idempotency and replay

The idempotency boundary is:

```text
partner + operation + Idempotency-Key
```

- same key and same canonical payload: replay stored response;
- same key and different payload: HTTP 409;
- nonce reuse inside the configured TTL: HTTP 409 `REPLAY_DETECTED`;
- `endToEndId` remains a distinct durable business reference.

## 5. Callback registration

The callback URL is provided per request but must match the partner allowlist.

Callbacks use:

- HTTPS/mTLS;
- `X-Correlation-ID`;
- detached JWS in `X-SIXPAY-Signature`;
- RS256 with a mandatory `kid`;
- at-least-once delivery;
- deduplication by `eventId`.

## 6. Public errors

TresorPay must branch on `status` and stable `code`, not on the human-readable
`detail`. `Retry-After` must be honored on 429 and retryable 503 responses.

## 7. Sandbox checklist

Before certification provide:

- base URL and DNS;
- token endpoint;
- sandbox client ID and credential method;
- accepted audience and scopes;
- mTLS client certificate procedure;
- trusted CA chain;
- source IP allowlist;
- callback connectivity;
- test customers/accounts;
- nominal, insufficient-funds, blocked-account and duplicate scenarios;
- support contact and availability window.

## 8. Production cutover

Production enablement requires:

- approved contract version;
- successful contract tests;
- mTLS handshake evidence;
- JWT claim validation evidence;
- callback JWS verification evidence;
- idempotency and replay evidence;
- rate-limit agreement;
- monitoring and incident contacts;
- certificate and signing-key rotation plan.
