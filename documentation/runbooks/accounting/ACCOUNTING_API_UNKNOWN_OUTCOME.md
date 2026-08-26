# Accounting API — Unknown submission outcome

When `POST /v1/accounting/batches` returns a transport ambiguity, 429 or 5xx:

1. keep the original SIXPAY `batchId`;
2. keep the original batch `idempotencyKey`;
3. do not build a replacement batch;
4. do not retry POST blindly;
5. query `/by-idempotency-key/{idempotencyKey}`;
6. if found, reconcile the local batch with the provider response;
7. if not found, apply the future operational reconciliation policy;
8. record correlation ID and request ID;
9. never log OAuth tokens, raw credentials or private keys.
