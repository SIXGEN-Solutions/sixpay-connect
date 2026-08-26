# TresorPay Simulated Client Runbook

## Local profile

For local contract testing where a real client certificate is unavailable:

```bash
export TRESORPAY_MTLS_REQUIRED=false
export TRESORPAY_OAUTH2_REQUIRED=false
export TRESORPAY_API_KEY_ENABLED=false
export TRESORPAY_CALLBACK_HOST=tresorpay.cm
```

This profile is forbidden for production.

## Request

```bash
curl --request POST 'http://localhost:8080/v1/payments/initiate'   --header 'Content-Type: application/json'   --header 'Idempotency-Key: TP-AVI-2026-00045678'   --header 'X-Correlation-ID: 4f7e9cbb-f6df-4ef4-bb22-d2d8266b39b0'   --header "X-Request-Timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"   --header 'X-Request-Nonce: 1722f93a-67de-4dbb-bca1-b40323085419'   --data @documentation/stubs/tresorpay/initiate-debit-request.json
```

## Expected nominal response

- HTTP 200;
- `X-Correlation-ID` response header;
- `Status=PENDING_CONFIRMATION`;
- stable payment reference;
- callback not executed inside the initiation transaction.

## Negative scenarios

1. resend the same nonce: expect `409 REPLAY_DETECTED`;
2. reuse the idempotency key with a changed amount: expect
   `409 IDEMPOTENCY_KEY_CONFLICT`;
3. use an unregistered callback host: expect
   `403 CALLBACK_URL_NOT_ALLOWED`;
4. exceed the configured quota: expect
   `429 RATE_LIMIT_EXCEEDED` with `Retry-After`;
5. change `LoginName`: expect
   `403 PARTNER_IDENTITY_MISMATCH`.

## Validation

```bash
cd backend
mvn -pl payment -am test
mvn -pl payment -am verify
```
