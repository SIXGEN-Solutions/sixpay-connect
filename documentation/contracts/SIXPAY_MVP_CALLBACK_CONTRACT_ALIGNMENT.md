# SIXPAY CONNECT — P0 Contract alignment: Payment Command and callback

Reference revision: `feat/repository-baseline-consolidation-cucumber` @ `498cbb3ae899880d2ee2489fdf0b4f8db4309091`.
Decision: the current tested implementation remains authoritative. The canonical callback publication contract is
`documentation/contracts/tresorpay/tresorpay-payment-callback-webhook-v1.yaml`.
The Payment Command API owns `POST /v1/payments/initiate`, **not** the callback payload schema.

## Callback: current published facts

- Events: `CUT_CREDITED`, `PAYMENT_REJECTED`, `TREASURY_INTEGRATED`; no terminal notification for a still-unknown T0 outcome.
- `CUT_CREDITED` establishes immediate T0 posting, not TFJ finality. Only a confirmed and uniquely matched TFJ result can produce `TREASURY_INTEGRATED`.
- The transport is at-least-once. Deduplicate by `eventId`; retry uses another `X-Webhook-Delivery-ID` and preserves the event payload.
- Partner durable acknowledgement: `HTTP 204` for the first or identical repeated event; `HTTP 409` for a conflicting event with the same ID.
- Sender headers explicitly set by `PaymentStatusCallbackHttpAdapter`: `X-Correlation-ID`, `X-Webhook-Event-ID`, `X-Webhook-Delivery-ID`, `X-Webhook-Delivery-Attempt`, `X-SIXPAY-Signature`, `X-SIXPAY-Signature-Key-ID`, `X-SIXPAY-Signature-Timestamp`.
- The signing implementation is `HMAC-SHA256`, not detached JWS. Signing string: `unixTimestamp + '.' + upper(method) + '.' + rawPathAndQuery + '.' + base64url(SHA256(rawBody))`; `X-SIXPAY-Signature` is `v1=` plus base64url(HMAC-SHA256(secret, signing string)).
- A partner must check signature and timestamp freshness on raw bytes before processing the JSON.
- The inspected HTTP adapter does **not** explicitly set Bearer authorization or `X-Subscription-Key`. The callback publication contract therefore does not require either. A global `RestClient.Builder` customizer or gateway could add transport behavior, but that has **not** been established here. Adding such a guarantee requires renewed verification and explicit approval.
- The actual adapter accepts any successful HTTP response via `.retrieve().toBodilessEntity()`; `204` is the **partner contract for durable acknowledgement**, not an HTTP 204-only check in the sender code.

## Payment initiation: `referenceClient`

- The authoritative semantic is `CustomerSubscriptionId`, UUID format. Valid tested sample: `6bed9760-82a2-4698-bcf7-70b111a0cbdd`.
- The Java request DTO currently uses `@NotBlank` and `@Size(max = 128)` for the string field; it does not have a Bean Validation UUID annotation. UUID conversion, subscription existence, ownership and authorization remain application/domain checks. Do not claim they are all enforced by the DTO.
- `referenceClient` is not `endToEndId`, a customer number, a RIB, or a partner business ID. `AppID` must identify the authenticated business Partner, separately from the transport application header.

## Rejection and error inventory — distinguish evidence from possibilities

**Documented in current Payment Command OpenAPI**: HTTP `400`, `401`, `403`, `409`, `422`, `429`, `503`. These codes describe the declared API boundary, not proof of every underlying exception mapping.

**Documented in current callback OpenAPI**: HTTP `400`, `401`, `403`, `409`, `422`, `429`, `500`, `502`, `503`, `504`; `204` is success. A non-success or transport error causes callback-delivery failure/retry policy, not financial replay.

**Documented operational email errors**: `SMTP_SEND_FAILED`, `SMTP_TEMPORARILY_UNAVAILABLE`, `SMTP_AUTHENTICATION_FAILED`, `SMTP_MESSAGE_INVALID`, `ADMIN_RECIPIENT_NOT_RESOLVED`, `EMAIL_TEMPLATE_INVALID`, `UNSUPPORTED_NOTIFICATION_CHANNEL`.

**To complete later from actual controllers, exception handlers and runtime evidence**: exact domain-to-HTTP mapping for nonexistent or inaccessible `CustomerSubscription`, duplicate `endToEndId`, conflicting idempotency, invalid beneficiary sum, bank unavailability, bank `UNKNOWN`, OTP expiry/locking, reconciliation and provider timeouts. Do not publish unverified partner-facing machine error codes.

## Security and operations

- Inbound payment command requires the separately governed M2M security boundary; outgoing webhook HMAC does not replace inbound M2M authentication.
- Callback delivery errors must not mutate the financial payment state. A retry of a callback is not a new banking POST.
- `notification-operational-email-v1` has been explicitly approved for the tested SMTP adapter; `ACCEPTED` is not mailbox-delivered.
- Production Keycloak placeholders and outstanding infrastructure decisions: see `documentation/runbooks/deploy_cloud_mvp/KEYCLOAK_PRODUCTION_PARAMETERS.md`.

## Contract policy

- Keep `payment-command-api-v1` active and approved. Keep the webhook registry records `ACTIVE_MVP`, `APPROVED`, `REFERENCE_ONLY`, `codeGenerationAllowed: false`.
- Remove obsolete inline callback **schema** from Payment Command, rather than retaining two conflicting sources of truth.
- This is a documentation-only decision and does not authorize generation of code from a `REFERENCE_ONLY` contract.
