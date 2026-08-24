# FS-2.5.0 — Configuration Inventory & Ownership Matrix

**Branch:** `feat/repository-baseline-consolidation`  
**Phase:** `FS-2.5 — Configuration consolidation`  
**Golden module:** Partner

## Objective

Inventory the current configuration surface before any move, rename or cleanup. Existing runtime behavior remains protected.

## Ownership policy

### Bootstrap/global

- `server.*`
- `spring.application.*`
- `spring.datasource.*`
- `spring.jpa.*`
- `spring.flyway.*`
- `springdoc.*`
- `management.*`
- global observability/logging
- authentication runtime/profile assembly
- provider-neutral messaging/runtime transport

### Domain-owned

- `sixpay.partner.*` → Partner
- `sixpay.customer.*` → Customer
- `sixpay.payment.*` → Payment
- `sixpay.accounting.*` → Accounting
- `sixpay.reporting.*` → Reporting
- `sixpay.notification.*` → Notification
- `sixpay.security.*` → Security policy/domain configuration
- `sixpay.administration.*` → Administration

Bootstrap may assemble domain configuration, but should not become its semantic owner.

## Backend YAML inventory

| File | Key | Suggested owner |
|---|---|---|
| `backend\bootstrap\src\main\resources\application-accounting-api-sandbox.yml` | `spring.config.activate.on-profile` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-accounting-api-sandbox.yml` | `sixpay.accounting.api.enabled` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.enabled` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.base-url` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.submit-path` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.batch-lookup-path` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.idempotency-lookup-path` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.connect-timeout` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.read-timeout` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.security.oauth2-registration-id` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.security.ssl-bundle` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting-api.yml` | `sixpay.accounting.api.contract.idempotency-header` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting.yml` | `sixpay.accounting.batch.cutoff-zone` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-accounting.yml` | `sixpay.accounting.batch.cutoff-time` | `ACCOUNTING` |
| `backend\bootstrap\src\main\resources\application-amplitude-payment-sandbox.yml` | `spring.config.activate.on-profile` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-amplitude-sandbox.yml` | `spring.config.activate.on-profile` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-amplitude-sandbox.yml` | `sixpay.customer.verification.banking.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.security.oauth2.client.provider.core-banking.token-uri` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.security.oauth2.client.registration.core-banking-customer-verification.provider` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.security.oauth2.client.registration.core-banking-customer-verification.authorization-grant-type` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.security.oauth2.client.registration.core-banking-customer-verification.client-id` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.security.oauth2.client.registration.core-banking-customer-verification.client-secret` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.security.oauth2.client.registration.core-banking-customer-verification.scope` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.ssl.bundle.jks.core-banking-client.keystore.location` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.ssl.bundle.jks.core-banking-client.keystore.password` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.ssl.bundle.jks.core-banking-client.keystore.type` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.ssl.bundle.jks.core-banking-client.truststore.location` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.ssl.bundle.jks.core-banking-client.truststore.password` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `spring.ssl.bundle.jks.core-banking-client.truststore.type` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.base-url` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.endpoint-path` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.connect-timeout` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.read-timeout` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.max-attempts` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.retry-backoff` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.evidence-ttl` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.security.oauth2-registration-id` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.security.ssl-bundle` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-banking.yml` | `sixpay.customer.verification.banking.contract.version` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.batch-size` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.polling-interval` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.max-attempts` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.initial-backoff` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.max-backoff` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-customer-projection-outbox.yml` | `sixpay.payment.outbox.customer-projection.processing-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `sixpay.security.authentication.local.maximum-failed-attempts` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `sixpay.security.authentication.local.lock-duration` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `sixpay.security.authentication.local.bcrypt-strength` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `sixpay.security.authentication.oidc.registration-id` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `server.servlet.session.timeout` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `server.servlet.session.cookie.http-only` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `server.servlet.session.cookie.secure` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-hybrid-auth.yml` | `server.servlet.session.cookie.same-site` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.enabled` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.client-id` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.topics.payment.received.v1` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.topics.payment.failed.v1` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.topics.payment.posted.v1` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.topics.payment.reversed.v1` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.topics.payment.reconciliation-required.v1` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.producer.idempotence` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.producer.acks` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.producer.retries` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.producer.max-in-flight-requests` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.consumer.auto-offset-reset` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.consumer.enable-auto-commit` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.consumer.concurrency` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.consumer.poll-timeout` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.outbox.batch-size` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.outbox.poll-interval` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.outbox.delivered-retention` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.outbox.cleanup-batch-size` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retry.first-delay` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retry.second-delay` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retry.third-delay` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retry.maximum-attempts` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retry.dead-letter-suffix` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retention.main` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retention.reconciliation` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retention.retry` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retention.dead-letter` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `sixpay.integration.kafka.retention.deduplication` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.bootstrap-servers` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.producer.key-serializer` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.producer.value-serializer` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.producer.properties.enable.idempotence` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.producer.properties.acks` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.producer.properties.max.in.flight.requests.per.connection` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.consumer.key-deserializer` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.consumer.value-deserializer` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.consumer.enable-auto-commit` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.consumer.auto-offset-reset` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration-kafka.yml` | `spring.kafka.listener.ack-mode` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.local.seed-enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.local.seed.admin-password` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.local.seed.manager-password` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.local.seed.auditor-password` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.local.seed.partner-password` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `sixpay.security.local.seed.partner-subject` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `server.servlet.session.timeout` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `server.servlet.session.cookie.http-only` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `server.servlet.session.cookie.same-site` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-integration.yml` | `server.servlet.session.cookie.secure` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-kafka.yml` | `spring.config.activate.on-profile` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-kafka.yml` | `sixpay.integration.kafka.enabled` | `INTEGRATION_SHARED` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `sixpay.security.authentication.local.maximum-failed-attempts` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `sixpay.security.authentication.local.lock-duration` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `sixpay.security.authentication.local.bcrypt-strength` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `server.servlet.session.timeout` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `server.servlet.session.cookie.http-only` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `server.servlet.session.cookie.secure` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-local-auth.yml` | `server.servlet.session.cookie.same-site` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-notification-operational.yml` | `sixpay.notification.operational.retry.enabled` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-notification-operational.yml` | `sixpay.notification.operational.retry.max-attempts` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-notification-operational.yml` | `sixpay.notification.operational.retry.initial-backoff` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-notification-operational.yml` | `sixpay.notification.operational.retry.max-backoff` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-notification-operational.yml` | `sixpay.notification.operational.retry.batch-size` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-notification-operational.yml` | `sixpay.notification.operational.retry.poll-interval-ms` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-oidc.yml` | `sixpay.security.mode` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-oidc.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-oidc.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-oidc.yml` | `sixpay.security.authentication.oidc.registration-id` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-oidc.yml` | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.base-url` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.release-path` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.reversal-path` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.connect-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.read-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.security.oauth2-registration-id` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.security.ssl-bundle` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.version` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.idempotency-header` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.release-success-codes` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.release-rejected-codes` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.reversal-success-codes` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.reversal-rejected-codes` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-compensation.yml` | `sixpay.payment.banking.amplitude.compensation.contract.reversal-not-allowed-codes` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.base-url` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.posting-path` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.connect-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.read-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.security.oauth2-registration-id` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.security.ssl-bundle` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.contract.version` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-posting.yml` | `sixpay.payment.banking.amplitude.posting.contract.idempotency-header` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.base-url` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.reservation-path` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.connect-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.read-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.security.oauth2-registration-id` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.security.ssl-bundle` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.contract.version` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-reservation.yml` | `sixpay.payment.banking.amplitude.reservation.contract.idempotency-header` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.base-url` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.by-idempotency-path` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.by-bank-reference-path` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.connect-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.read-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.security.oauth2-registration-id` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-payment-banking-status.yml` | `sixpay.payment.banking.amplitude.status.security.ssl-bundle` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `BOOTSTRAP_AUTH_RUNTIME` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `sixpay.security.mode` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `sixpay.security.authentication.oidc.registration-id` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-secured.yml` | `sixpay.messaging.outbox.enabled` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.url` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.username` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.password` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.hikari.schema` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.hikari.maximum-pool-size` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.hikari.minimum-idle` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.hikari.idle-timeout` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.hikari.connection-timeout` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.datasource.hikari.max-lifetime` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.database-platform` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.show-sql` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.properties.hibernate.format_sql` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.properties.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.host` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.port` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.username` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.password` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.properties.mail.smtp.auth` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.properties.mail.smtp.starttls.enable` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.properties.mail.smtp.starttls.required` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.properties.mail.smtp.connectiontimeout` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.properties.mail.smtp.timeout` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `spring.mail.properties.mail.smtp.writetimeout` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `springdoc.api-docs.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `springdoc.swagger-ui.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `springdoc.swagger-ui.path` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `springdoc.swagger-ui.groups-order` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `springdoc.swagger-ui.operations-sorter` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `springdoc.swagger-ui.tags-sorter` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `management.endpoints.web.exposure.include` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.messaging.outbox.enabled` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.email.mode` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.email.from` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.email.subject-prefix` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.email.enabled` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.email.from` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.email.subject-prefix` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.email.admin-recipients.email` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.email.admin-recipients.locale` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.email.admin-recipients.enabled` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.retry.enabled` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.retry.max-attempts` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.retry.initial-backoff` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.retry.max-backoff` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.retry.batch-size` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.retry.poll-interval-ms` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.metrics-enabled` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.metrics-refresh-ms` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.retention-enabled` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.delivered-retention` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.failed-retention` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.purge-batch-size` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.notification.operational.operations.purge-interval-ms` | `NOTIFICATION` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.mtls-required` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.oauth2-required` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.api-key-enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.api-key-header` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.api-key-value` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.audience` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.partner-claim` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.security.required-scope` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.anti-replay.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.anti-replay.allowed-clock-skew` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.anti-replay.nonce-ttl` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.rate-limit.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.rate-limit.requests-per-minute` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.callback.signature-enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.callback.algorithm` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.tresorpay.callback.delivery-expiration` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.poll-delay` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.batch-size` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.max-attempts` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.claim-timeout` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.initial-retry-delay` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.maximum-retry-delay` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.worker-id` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.signing-key-id` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.payment.callback.signing-private-key-pem` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.verification.banking.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.persistence.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.persistence.max-optimistic-attempts` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.persistence.protection-key-base64` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.query.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.query.cursor-key-base64` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.audit.persistence.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.resilience.max-attempts` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.resilience.initial-backoff` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.resilience.max-backoff` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.resilience.multiplier` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.customer.observation.resilience.jitter` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.reporting.audit-query.cursor-hmac-key` | `REPORTING` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.reporting.audit-export.storage-directory` | `REPORTING` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.reporting.audit-export.retrieval-base-uri` | `REPORTING` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.reporting.audit-export.retention` | `REPORTING` |
| `backend\bootstrap\src\main\resources\application-standalone.yml` | `sixpay.reporting.audit-export.recovery-delay-ms` | `REPORTING` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.mtls-required` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.oauth2-required` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.api-key-enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.api-key-header` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.api-key-value` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.audience` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.partner-claim` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.security.required-scope` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.anti-replay.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.anti-replay.allowed-clock-skew` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.anti-replay.nonce-ttl` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.rate-limit.enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.rate-limit.requests-per-minute` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.callback.signature-enabled` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.callback.algorithm` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application-tresorpay.yml` | `sixpay.payment.tresorpay.callback.delivery-expiration` | `PAYMENT` |
| `backend\bootstrap\src\main\resources\application.yml` | `server.port` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.application.name` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.schemas` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.default-schema` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.locations` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.validate-on-migrate` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.clean-disabled` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `spring.flyway.out-of-order` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `springdoc.api-docs.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `springdoc.swagger-ui.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `management.endpoints.web.exposure.include` | `BOOTSTRAP_GLOBAL` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.messaging.outbox.enabled` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.customer.verification.banking.enabled` | `CUSTOMER` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.security.local.password.min-length` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.security.local.password.max-length` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.security.local.password.history-size` | `SECURITY` |
| `backend\bootstrap\src\main\resources\application.yml` | `sixpay.security.local.password.expiration-days` | `SECURITY` |
| `backend\customer\src\main\resources\application-resilience-example.yml` | `sixpay.customer.observation.resilience.max-attempts` | `CUSTOMER` |
| `backend\customer\src\main\resources\application-resilience-example.yml` | `sixpay.customer.observation.resilience.initial-backoff` | `CUSTOMER` |
| `backend\customer\src\main\resources\application-resilience-example.yml` | `sixpay.customer.observation.resilience.max-backoff` | `CUSTOMER` |
| `backend\customer\src\main\resources\application-resilience-example.yml` | `sixpay.customer.observation.resilience.multiplier` | `CUSTOMER` |
| `backend\customer\src\main\resources\application-resilience-example.yml` | `sixpay.customer.observation.resilience.jitter` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.base-url` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.endpoint-path` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.connect-timeout` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.read-timeout` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.max-attempts` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.retry-backoff` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.evidence-ttl` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.security.oauth2-registration-id` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-customer-verification-example.yml` | `sixpay.customer.verification.banking.security.ssl-bundle` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-test.yml` | `sixpay.customer.verification.banking.enabled` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-test.yml` | `sixpay.customer.observation.persistence.enabled` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-test.yml` | `sixpay.customer.observation.audit.persistence.enabled` | `CUSTOMER` |
| `backend\customer\src\test\resources\application-test.yml` | `sixpay.customer.observation.query.enabled` | `CUSTOMER` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.datasource.hikari.maximum-pool-size` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.database-platform` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.show-sql` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.format_sql` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.flyway.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.flyway.schemas` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.flyway.default-schema` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.flyway.locations` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.flyway.validate-on-migrate` | `BOOTSTRAP_GLOBAL` |
| `backend\notification\src\test\resources\application-test.yml` | `spring.flyway.clean-disabled` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.datasource.hikari.schema` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.database-platform` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.show-sql` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.format_sql` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.flyway.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.flyway.schemas` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.flyway.default-schema` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.flyway.locations` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.flyway.validate-on-migrate` | `BOOTSTRAP_GLOBAL` |
| `backend\partner\src\test\resources\application-test.yml` | `spring.flyway.clean-disabled` | `BOOTSTRAP_GLOBAL` |
| `backend\payment\src\test\resources\application-test.yml` | `sixpay.payment.tresorpay.enabled` | `PAYMENT` |
| `backend\reporting\src\test\resources\application-test.yml` | `sixpay.reporting.audit-query.cursor-hmac-key` | `REPORTING` |
| `backend\reporting\src\test\resources\application-test.yml` | `sixpay.reporting.audit-export.storage-directory` | `REPORTING` |
| `backend\reporting\src\test\resources\application-test.yml` | `sixpay.reporting.audit-export.retrieval-base-uri` | `REPORTING` |
| `backend\reporting\src\test\resources\application-test.yml` | `sixpay.reporting.audit-export.retention` | `REPORTING` |
| `backend\reporting\src\test\resources\application-test.yml` | `sixpay.reporting.audit-export.recovery-delay-ms` | `REPORTING` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `spring.application.name` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `spring.flyway.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `spring.jpa.properties.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `sixpay.customer.observation.persistence.enabled` | `CUSTOMER` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `sixpay.customer.observation.persistence.protection-key-base64` | `CUSTOMER` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `sixpay.customer.observation.query.enabled` | `CUSTOMER` |
| `backend\tests\src\payment-customer-test\resources\application-payment-customer-test.yml` | `sixpay.customer.observation.audit.persistence.enabled` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.application.name` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.jpa.properties.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.schemas` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.default-schema` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.locations` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.validate-on-migrate` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.clean-disabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `spring.flyway.out-of-order` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `springdoc.api-docs.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `springdoc.swagger-ui.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.messaging.outbox.enabled` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.customer.verification.banking.enabled` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.customer.observation.persistence.enabled` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.customer.observation.persistence.protection-key-base64` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.customer.observation.query.enabled` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.customer.observation.query.cursor-key-base64` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.customer.observation.audit.persistence.enabled` | `CUSTOMER` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.reporting.audit-query.cursor-hmac-key` | `REPORTING` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.reporting.audit-export.storage-directory` | `REPORTING` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.reporting.audit-export.retrieval-base-uri` | `REPORTING` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.reporting.audit-export.retention` | `REPORTING` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.reporting.audit-export.recovery-delay-ms` | `REPORTING` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.notification.retry.enabled` | `NOTIFICATION` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\tests\src\test\resources\application-assembled-test.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.datasource.driver-class-name` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.datasource.hikari.schema` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.jpa.open-in-view` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.jpa.database-platform` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.jpa.hibernate.ddl-auto` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.jpa.properties.hibernate.default_schema` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.jpa.properties.hibernate.jdbc.time_zone` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.flyway.enabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.flyway.schemas` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.flyway.default-schema` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.flyway.locations` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.flyway.validate-on-migrate` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `spring.flyway.clean-disabled` | `BOOTSTRAP_GLOBAL` |
| `backend\tests\src\test\resources\application-e2e.yml` | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\tests\src\test\resources\application-e2e.yml` | `sixpay.messaging.outbox.enabled` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\tests\src\test\resources\application-e2e.yml` | `sixpay.messaging.outbox.batch-size` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\tests\src\test\resources\application-e2e.yml` | `sixpay.messaging.outbox.polling-delay` | `BOOTSTRAP_RUNTIME_SHARED` |
| `backend\tests\src\test\resources\application-e2e.yml` | `sixpay.notification.retry.enabled` | `NOTIFICATION` |
| `backend\tests\src\test\resources\application-e2e.yml` | `sixpay.security.mode` | `SECURITY` |
| `backend\tests\src\test\resources\application-hybrid-security-assembled.yml` | `sixpay.security.authentication.local.enabled` | `SECURITY` |
| `backend\tests\src\test\resources\application-hybrid-security-assembled.yml` | `sixpay.security.authentication.oidc.enabled` | `SECURITY` |
| `backend\tests\src\test\resources\application-hybrid-security-assembled.yml` | `sixpay.security.authentication.oidc.registration-id` | `SECURITY` |

## Java configuration consumers

| Module | Kind | Key/prefix | Suggested owner | Source |
|---|---|---|---|---|
| bootstrap | Value | `sixpay.security.local.seed.admin-password` | `SECURITY` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\security\IntegrationSecurityUserSeeder.java` |
| bootstrap | Value | `sixpay.security.local.seed.manager-password` | `SECURITY` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\security\IntegrationSecurityUserSeeder.java` |
| bootstrap | Value | `sixpay.security.local.seed.auditor-password` | `SECURITY` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\security\IntegrationSecurityUserSeeder.java` |
| bootstrap | Value | `sixpay.security.local.seed.partner-password` | `SECURITY` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\security\IntegrationSecurityUserSeeder.java` |
| bootstrap | Value | `sixpay.security.local.seed.partner-subject` | `SECURITY` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\security\IntegrationSecurityUserSeeder.java` |
| bootstrap | ConditionalOnProperty | `sixpay.security.local.seed-enabled` | `SECURITY` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\security\IntegrationSecurityUserSeeder.java` |
| bootstrap | Value | `sixpay.e2e.customer.amplitude-base-url` | `REVIEW_REQUIRED` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\integration\e2e\CustomerE2eConfiguration.java` |
| bootstrap | ConditionalOnProperty | `sixpay.e2e.customer.enabled` | `REVIEW_REQUIRED` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\integration\e2e\CustomerE2eConfiguration.java` |
| bootstrap | ConfigurationProperties | `sixpay.payment.outbox.customer-projection` | `PAYMENT` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\integration\customer\outbox\CustomerProjectionOutboxProperties.java` |
| bootstrap | ConditionalOnProperty | `sixpay.payment.outbox.customer-projection.enabled` | `PAYMENT` | `backend\bootstrap\src\main\java\com\sixpay\bootstrap\integration\customer\outbox\PaymentObservedCustomerOutboxConfiguration.java` |
| customer | ConditionalOnProperty | `sixpay.customer.observation.audit.persistence.enabled` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\configuration\ObservedCustomerAuditPersistenceConfiguration.java` |
| customer | ConditionalOnProperty | `sixpay.customer.observation.persistence.enabled` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\configuration\ObservedCustomerPersistenceConfiguration.java` |
| customer | ConfigurationProperties | `sixpay.customer.observation.persistence` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\configuration\ObservedCustomerPersistenceProperties.java` |
| customer | ConfigurationProperties | `sixpay.customer.observation.resilience` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\configuration\ObservedCustomerProjectionResilienceProperties.java` |
| customer | ConditionalOnProperty | `sixpay.customer.observation.query.enabled` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\configuration\ObservedCustomerQueryConfiguration.java` |
| customer | ConfigurationProperties | `sixpay.customer.observation.query` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\configuration\ObservedCustomerQueryProperties.java` |
| customer | ConditionalOnProperty | `sixpay.customer.observation.query.enabled` | `CUSTOMER` | `backend\customer\src\main\java\com\sixpay\customer\observation\api\configuration\ObservedCustomerQueryApiConfiguration.java` |
| integration | ConditionalOnProperty | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\integration\src\main\java\com\sixpay\integration\configuration\InternalMessagingAutoConfiguration.java` |
| integration | ConditionalOnProperty | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\integration\src\main\java\com\sixpay\integration\configuration\KafkaMessagingAutoConfiguration.java` |
| integration | ConditionalOnProperty | `sixpay.messaging.outbox.enabled` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\integration\src\main\java\com\sixpay\integration\configuration\OutboxRelayAutoConfiguration.java` |
| integration | ConfigurationProperties | `sixpay.messaging.kafka` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\integration\src\main\java\com\sixpay\integration\messaging\properties\KafkaMessagingProperties.java` |
| integration | ConfigurationProperties | `sixpay.messaging.outbox` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\integration\src\main\java\com\sixpay\integration\messaging\properties\OutboxRelayProperties.java` |
| notification | ConditionalOnProperty | `sixpay.notification.email.mode` | `NOTIFICATION` | `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationEmailAutoConfiguration.java` |
| notification | ConditionalOnProperty | `sixpay.notification.email.mode` | `NOTIFICATION` | `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationEmailAutoConfiguration.java` |
| notification | ConditionalOnProperty | `sixpay.notification.email.mode` | `NOTIFICATION` | `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationEmailAutoConfiguration.java` |
| notification | ConditionalOnProperty | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationMessagingAutoConfiguration.java` |
| notification | ConditionalOnProperty | `sixpay.messaging.transport` | `BOOTSTRAP_RUNTIME_SHARED` | `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationMessagingAutoConfiguration.java` |
| notification | ConditionalOnProperty | `sixpay.notification.retry.enabled` | `NOTIFICATION` | `backend\notification\src\main\java\com\sixpay\notification\configuration\NotificationRetryAutoConfiguration.java` |
| notification | ConfigurationProperties | `sixpay.notification.email` | `NOTIFICATION` | `backend\notification\src\main\java\com\sixpay\notification\infrastructure\email\NotificationEmailProperties.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\PaymentCallbackDetachedJwsSigner.java` |
| payment | ConfigurationProperties | `sixpay.payment.callback` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\PaymentCallbackProperties.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\PaymentCallbackSchedulingConfiguration.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\PaymentCallbackSigningConfiguration.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\PaymentStatusCallbackHttpAdapter.java` |
| payment | ConditionalOnProperty | `sixpay.payment.tresorpay.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\tresorpay\TresorPayIntegrationConfiguration.java` |
| payment | ConfigurationProperties | `sixpay.payment.tresorpay` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\tresorpay\TresorPayIntegrationProperties.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\relay\PaymentCallbackOutboxCoordinator.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\relay\PaymentCallbackOutboxRelay.java` |
| payment | ConditionalOnProperty | `sixpay.payment.callback.enabled` | `PAYMENT` | `backend\payment\src\main\java\com\sixpay\payment\infrastructure\callback\relay\PaymentCallbackPlanFactory.java` |
| reporting | ConfigurationProperties | `sixpay.reporting.audit-export` | `REPORTING` | `backend\reporting\src\main\java\com\sixpay\reporting\configuration\ReportingAuditExportProperties.java` |
| reporting | ConfigurationProperties | `sixpay.reporting.audit-query` | `REPORTING` | `backend\reporting\src\main\java\com\sixpay\reporting\configuration\ReportingAuditQueryProperties.java` |
| security | ConditionalOnProperty | `sixpay.security.authentication-mode` | `SECURITY` | `backend\security\src\main\java\com\sixpay\security\config\OidcSecurityConfiguration.java` |
| security | ConfigurationProperties | `sixpay.security` | `SECURITY` | `backend\security\src\main\java\com\sixpay\security\config\SixpaySecurityProperties.java` |
| security | ConditionalOnProperty | `sixpay.security.authentication.local.enabled` | `SECURITY` | `backend\security\src\main\java\com\sixpay\security\configuration\LocalAuthenticationConfiguration.java` |
| security | ConditionalOnProperty | `sixpay.security.authentication.oidc.enabled` | `SECURITY` | `backend\security\src\main\java\com\sixpay\security\configuration\SixpaySecurityAutoConfiguration.java` |
| security | ConditionalOnProperty | `sixpay.security.authentication.local.enabled` | `SECURITY` | `backend\security\src\main\java\com\sixpay\security\api\controller\LocalAuthenticationController.java` |
| security | ConditionalOnProperty | `sixpay.security.authentication.local.enabled` | `SECURITY` | `backend\security\src\main\java\com\sixpay\security\api\controller\LocalPasswordController.java` |

## Angular environment inventory

| File | Detected concerns | Target ownership |
|---|---|---|
| `frontend\src\environments\authentication-environment.spec.ts` | `production`, `backend`, `mode`, `authentication`, `local`, `oidc`, `authority`, `clientId`, `scope`, `apiBaseUrl` | `FRONTEND_AUTH_RUNTIME` |
| `frontend\src\environments\authentication-environment.ts` | `production`, `authentication`, `local`, `oidc`, `authority`, `clientId`, `scope` | `FRONTEND_AUTH_RUNTIME` |
| `frontend\src\environments\environment.development.ts` | `production`, `backend`, `mode`, `authentication`, `local`, `oidc`, `apiBaseUrl` | `FRONTEND_RUNTIME_GLOBAL` |
| `frontend\src\environments\environment.integration.ts` | `production`, `backend`, `mode`, `authentication`, `local`, `oidc`, `apiBaseUrl` | `FRONTEND_RUNTIME_GLOBAL` |
| `frontend\src\environments\environment.model.ts` | `production`, `backend`, `mode`, `authentication`, `local`, `oidc`, `authority`, `clientId`, `scope`, `apiBaseUrl` | `FRONTEND_RUNTIME_GLOBAL` |
| `frontend\src\environments\environment.netlify.ts` | `production`, `backend`, `mode`, `authentication`, `local`, `oidc`, `apiBaseUrl` | `FRONTEND_RUNTIME_GLOBAL` |
| `frontend\src\environments\environment.ts` | `production`, `backend`, `mode`, `authentication`, `local`, `oidc`, `authority`, `clientId`, `scope`, `apiBaseUrl` | `FRONTEND_RUNTIME_GLOBAL` |

## Bootstrap domain-configuration debt

No domain-owned configuration key found in Bootstrap.

## Initial classification counts

- `ACCOUNTING`: **13** leaf properties
- `BOOTSTRAP_AUTH_RUNTIME`: **11** leaf properties
- `BOOTSTRAP_GLOBAL`: **136** leaf properties
- `BOOTSTRAP_RUNTIME_SHARED`: **22** leaf properties
- `CUSTOMER`: **53** leaf properties
- `INTEGRATION_SHARED`: **30** leaf properties
- `NOTIFICATION`: **30** leaf properties
- `PAYMENT`: **92** leaf properties
- `REPORTING`: **15** leaf properties
- `SECURITY`: **37** leaf properties

## Consolidation principles

1. Configuration ownership follows capability ownership.
2. Bootstrap owns runtime assembly, not business semantics.
3. Moving a key must not change its effective property name or default during the consolidation phase.
4. `application-*.yml` profile semantics must remain backward compatible until replacement profiles are validated.
5. Frontend integration/production environments never silently fallback to mocks.
6. OpenAPI grouping belongs to Bootstrap runtime assembly when it groups multiple module controllers; domain-specific API metadata stays with the domain.
7. Security runtime assembly belongs to Bootstrap; password policy, authorization vocabulary and Security-owned feature controls remain Security-owned.
8. Every move requires targeted tests plus full backend/frontend verification.

## Suggested FS-2.5 sequence

- FS-2.5.0 — Inventory & ownership matrix
- FS-2.5.1 — Bootstrap/global configuration normalization
- FS-2.5.2 — Domain configuration ownership
- FS-2.5.3 — Profile consolidation (`standalone`, test, integration, sandbox)
- FS-2.5.4 — Security/authentication configuration consolidation
- FS-2.5.5 — OpenAPI/Springdoc configuration consolidation
- FS-2.5.6 — Angular environment consolidation
- FS-2.5.7 — Feature-flag registry and ownership
- FS-2.5.8 — Configuration non-regression gate
- FS-2.5.9 — Final validation

## Non-regression rule

FS-2.5 is classification-first. No property key, environment variable name, default value, active profile behavior or feature flag is changed merely for cosmetic consistency.
