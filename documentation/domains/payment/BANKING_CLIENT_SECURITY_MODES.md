# Payment — optional banking HTTP security

The existing Confirmation model is reused for Reservation, Posting (POST,
context and recovery) and Compensation (release and reversal).

Defaults for all three capabilities remain secure: `oauth2-enabled: true`,
`mtls-enabled: true`, and `allow-insecure-http: false`.

When OAuth2 is enabled, an `OAuth2AuthorizedClientManager` must exist; otherwise
startup fails. When OAuth2 is disabled, no `Authorization` header is sent.
When mTLS is disabled, the SSL bundle is not requested. Plain HTTP requires
an explicit `allow-insecure-http: true`, and cannot be combined with OAuth2
or mTLS. The bank must authorize any such transport mode explicitly.

For a bank sandbox explicitly permitting unauthenticated local HTTP, configure:

```dotenv
AMPLITUDE_FUNDS_RESERVATION_OAUTH2_ENABLED=false
AMPLITUDE_FUNDS_RESERVATION_MTLS_ENABLED=false
AMPLITUDE_FUNDS_RESERVATION_ALLOW_INSECURE_HTTP=true
AMPLITUDE_POSTING_OAUTH2_ENABLED=false
AMPLITUDE_POSTING_MTLS_ENABLED=false
AMPLITUDE_POSTING_ALLOW_INSECURE_HTTP=true
AMPLITUDE_COMPENSATION_OAUTH2_ENABLED=false
AMPLITUDE_COMPENSATION_MTLS_ENABLED=false
AMPLITUDE_COMPENSATION_ALLOW_INSECURE_HTTP=true
```

For HTTPS without OAuth2, keep mTLS on when the Core Banking requires it.
The `payment-banking-local` profile does not import the OAuth2 client
registration file; if an OAuth2-enabled mode is selected, supply a compatible
registration and manager through deployment configuration.

Never manually replay financial posting or compensation without first
checking its authoritative banking result; Outbox retries can resume after
restart. No API contract or financial event payload changes are intended.
